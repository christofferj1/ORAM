package oram.lookahead;


import oram.AccessStrategy;
import oram.Constants;
import oram.OperationType;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockLookahead;
import oram.block.BlockTrivial;
import oram.blockenc.BlockEncryptionStrategyLookahead;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.factory.Factory;
import oram.permutation.PermutationStrategy;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 04-03-2019. <br>
 * Master Thesis 2019 </p>
 * <p>
 * Without keeping the stash locally
 */

public class AccessStrategyLookaheadTrivial implements AccessStrategy {
    private final Logger logger = LogManager.getLogger("log");
    private final SecretKey secretKey;
    private final int size;
    private final int matrixHeight; // Assumed to be equal to matrix width
    private final CommunicationStrategy communicationStrategy;
    private final EncryptionStrategy encryptionStrategy;
    private final PermutationStrategy permutationStrategy;
    private final BlockEncryptionStrategyLookahead blockEncStrategy;
    private Map<Integer, Integer> positionMap;
    private int accessCounter;
    private List<SwapPartnerData> futureSwapPartners;
    private int offset;
    private String prefix;
    private int positionMapOffSet;
    private boolean uploadPositionMap;

    public AccessStrategyLookaheadTrivial(int size, int matrixHeight, byte[] key, Factory factory, int offset,
                                          AccessStrategy accessStrategy, int prefixSize) {
        this.size = size;
        this.matrixHeight = matrixHeight;
        this.communicationStrategy = factory.getCommunicationStrategy();
        this.encryptionStrategy = factory.getEncryptionStrategy();
        this.secretKey = encryptionStrategy.generateSecretKey(key);
        this.permutationStrategy = factory.getPermutationStrategy();
        blockEncStrategy = factory.getBlockEncryptionStrategyLookahead();
        this.offset = offset;
        this.prefix = Util.getEmptyStringOfLength(prefixSize);
        positionMapOffSet = (int) (offset + size + 2 * Math.sqrt(size));
        if (!(size == matrixHeight * matrixHeight))
            logger.error("Size of matrix is wrong");
        accessCounter = 0;
        futureSwapPartners = new ArrayList<>();
        positionMap = new HashMap<>();

        if (accessStrategy != null)
            uploadPositionMap = true;

        logger.info("######### Initialized Lookahead ORAM strategy #########");
        logger.debug("######### Initialized Lookahead ORAM strategy #########");
    }

    @Override
    public boolean setup(List<BlockTrivial> blocksGiven) {
        List<BlockTrivial> blocks = new ArrayList<>(blocksGiven);
        Util.logAndPrint(logger, prefix + "Starting setup");
//        Fill with dummy blocks
        for (int i = blocks.size(); i < size; i++) {
            blocks.add(new BlockTrivial(0, new byte[0]));
        }

        for (int i = blocks.size(); i > size; i--) {
            blocks.remove(i - 1);
        }

        Util.logAndPrint(logger, prefix + "    Created dummy blocks");

//        Shuffle and convert
        blocks = permutationStrategy.permuteTrivialBlocks(blocks);
        List<BlockLookahead> blockLookaheads = trivialToLookaheadBlocksForSetup(blocks);

//        Pick swap partners
        SecureRandom randomness = new SecureRandom();
        List<BlockLookahead> swapPartners = new ArrayList<>();
        for (int i = 0; i < matrixHeight; i++) {
            Index index = null;
            boolean indexIsNotUnique = true;
            while (indexIsNotUnique) {
                index = new Index(randomness.nextInt(matrixHeight), randomness.nextInt(matrixHeight));
                Index finalIndex = index;
                indexIsNotUnique = futureSwapPartners.stream().anyMatch(s -> s.getIndex().equals(finalIndex));
            }
            futureSwapPartners.add(new SwapPartnerData(index, accessCounter));
            accessCounter++;

            BlockLookahead block = blockLookaheads.get(getFlatArrayIndex(index));
            if (Util.isDummyAddress(block.getAddress()))
                block.setData(Util.getRandomByteArray(Constants.BLOCK_SIZE));
            swapPartners.add(block);
        }

        Util.logAndPrint(logger, prefix + "    Swap partners picked");

        List<Integer> addresses = new ArrayList<>();

//        Add blocks to the right place in the flattened array
//        First the matrix
        List<BlockLookahead> res = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            BlockLookahead block = blockLookaheads.get(i);
            Index index = block.getIndex();
            boolean isSwapPartner = futureSwapPartners.stream().anyMatch(s -> s.getIndex().equals(index));
            if (!isSwapPartner) {
//                There are added dummy blocks for the permutation, they should not be written to the database
                if (!Util.isDummyAddress(block.getAddress())) {
                    res.add(block);
                    addresses.add(i + offset);
                }
            }
        }
        futureSwapPartners = new ArrayList<>();

//        At last the swap stash
        for (int i = (size + matrixHeight); i < (size + matrixHeight * 2); i++) {
            BlockLookahead swapPartner = swapPartners.get(i - (size + matrixHeight));
            res.add(swapPartner);
            addresses.add(i + offset);
        }

        Util.logAndPrint(logger, prefix + "    Blocks added to final list");

//        Encrypt and write blocks to server
        List<BlockEncrypted> encryptedList = blockEncStrategy.encryptBlocks(res, secretKey);
        if (encryptedList.isEmpty()) {
            logger.error(prefix + "Unable to decrypt when initializing the ORAM");
            return false;
        }

        Util.logAndPrint(logger, prefix + "    Blocks encrypted");

        if (!communicationStrategy.writeArray(addresses, encryptedList)) {
            logger.error(prefix + "Writing blocks were unsuccessful when initializing the ORAM");
            return false;
        }

        if (uploadPositionMap) {
            if (writePositionMapFailed())
                return false;
            positionMap = null;
        } else {
            positionMap.put(0, -42);
        }

        Util.logAndPrint(logger, prefix + "    Blocks written to server");

        return true;
    }

    private boolean writePositionMapFailed() {
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(positionMap.entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));

        List<Integer> addresses = new ArrayList<>();
        List<BlockEncrypted> encryptedBlocks = new ArrayList<>();

        int positionMapBlocks = (int) Math.ceil((double) size / Constants.POSITION_BLOCK_SIZE);
        for (int i = 0; i < positionMapBlocks; i++) {
            Map<Integer, Integer> map = new HashMap<>();
            for (int j = 0; j < Constants.POSITION_BLOCK_SIZE; j++) {
                int index = i * Constants.POSITION_BLOCK_SIZE + j;

                if (entries.size() > index) // Fill the rest with dummy mappings
                    map.put(entries.get(index).getKey(), entries.get(index).getValue());
                else
                    map.put(index + 1, -42);
            }
            addresses.add(positionMapOffSet + i);
            byte[] encryptedData = encryptionStrategy.encrypt(Util.getByteArrayFromMap(map), secretKey);
            byte[] addressBytes = Util.getRandomByteArray(Constants.ENCRYPTED_INTEGER_SIZE);
            encryptedBlocks.add(new BlockEncrypted(addressBytes, encryptedData));
        }
        return !communicationStrategy.writeArray(addresses, encryptedBlocks);
    }

    private boolean readPositionMap() {
        int positionMapBlocks = (int) Math.ceil((double) size / Constants.POSITION_BLOCK_SIZE);
        List<Integer> addresses = IntStream.range(positionMapOffSet, positionMapOffSet + positionMapBlocks)
                .boxed().collect(Collectors.toList());
        List<BlockEncrypted> encryptedBlocks = communicationStrategy.readArray(addresses);
        if (encryptedBlocks == null) {
            logger.error("Unable to read position map from server");
            return false;
        }

        positionMap = new HashMap<>();
        for (int i = 0; i < positionMapBlocks; i++) {
            byte[] bytes = encryptionStrategy.decrypt(encryptedBlocks.get(i).getData(), secretKey);
            if (bytes == null) {
                logger.error("Unable to decrypt bytes for array (index: " + i + ")");
                return false;
            }
            Map<Integer, Integer> tmp = Util.getMapFromByteArray(bytes);
            if (tmp == null) {
                logger.error("Unable to get map from byte array");
                return false;
            }
            positionMap.putAll(tmp);
        }
        return true;
    }

    @Override
    public byte[] access(OperationType op, int address, byte[] data, boolean recursiveLookup, boolean lookaheadSetup) {
        int addressToLookUp = address;
        if (recursiveLookup)
            addressToLookUp = (int) Math.ceil((double) address / Constants.POSITION_BLOCK_SIZE);

        Integer position;
        if (positionMap == null) {
            if (!readPositionMap()) {
                logger.error("Failed to read position map");
                return null;
            }

            Integer flatArrayIndex = positionMap.getOrDefault(addressToLookUp, null);

            if (flatArrayIndex == null) {
                logger.error(prefix + "Unable to look up address: " + addressToLookUp);
                return null;
            }
            position = flatArrayIndex;
        } else
            position = positionMap.getOrDefault(addressToLookUp, null);

        if (position == null) {
            logger.error(prefix + "Unable to look up position for address: " + addressToLookUp);
            return null;
        }
        Index indexOfCurrentAddress = getIndexFromFlatArrayIndex(position);
        int maintenanceColumnIndex = Math.floorMod(accessCounter, matrixHeight);

        logger.info(prefix + "Access op: " + op.toString() + ", address: " + addressToLookUp + ", index: ("
                + indexOfCurrentAddress.getRowIndex() + ", " + indexOfCurrentAddress.getColIndex() +
                "), maintenance column: " + maintenanceColumnIndex);

//        This tells if the block we fetch is in the column used for maintenance
        boolean blockInColumn = indexOfCurrentAddress.getColIndex() == maintenanceColumnIndex;

        List<BlockLookahead> blocks = readBlocks(indexOfCurrentAddress, maintenanceColumnIndex, blockInColumn);
        if (blocks == null) {
            logger.error(prefix + "Blocks read from server were null");
            return null;
        }

//        If the block is found in the column, we fetch one less
        int numberOfBlocksToFetch = blockInColumn ? matrixHeight * 3 : matrixHeight * 3 + 1;
        if (blocks.size() != numberOfBlocksToFetch) {
            logger.error(prefix + "The number of blocks read from the server: " + blocks.size() + " should be: "
                    + numberOfBlocksToFetch);
        }

//        Fetch column and stashes from fetched blocks
        List<BlockLookahead> column = getColumn(blocks, blockInColumn, maintenanceColumnIndex);
        Map<Integer, Map<Integer, BlockLookahead>> accessStash = getAccessStash(blocks, blockInColumn);
        BlockLookahead[] swapStash = getSwapStash(blocks, blockInColumn);

//        Fetch block from either matrix, access stash or swap stash
        BlockLookahead block;
        if (blockInColumn)
            block = blocks.get(indexOfCurrentAddress.getRowIndex());
        else
            block = blocks.get(0);

        boolean blockFoundInMatrix = true;
        boolean blockFoundInAccessStash = true;
        int swapCount = 0;
        if (Util.isDummyAddress(block.getAddress())) {
            blockFoundInMatrix = false;
            block = findBlockInAccessStash(accessStash, indexOfCurrentAddress);
            if (block == null) {
                blockFoundInAccessStash = false;
                Pair<BlockLookahead, Integer> pair = findBlockInSwapStash(swapStash, addressToLookUp);
                if (pair == null) {
                    logger.error(prefix + "Unable to locate block, address: " + addressToLookUp);
                    return null;
                } else {
                    block = pair.getKey();
                    swapCount = pair.getValue();
                    logger.debug(prefix + "Block found in swap stash: " + block.toStringShort());
                }
            } else {
                logger.debug(prefix + "Block found in access stash: " + block.toStringShort());
            }
        } else {
            logger.debug(prefix + "Block found in matrix: " + block.toStringShort());
        }

//        Get swap partner
        BlockLookahead swapPartner = swapStash[maintenanceColumnIndex];
        swapStash[maintenanceColumnIndex] = null;

//        Set index to index of swap partner and add to access stash
        block.setIndex(swapPartner.getIndex());
        accessStash = addToAccessStashMap(accessStash, block);

//        Update swap partner index and encrypt it
        swapPartner.setIndex(indexOfCurrentAddress);
        BlockEncrypted encryptedSwapPartner = blockEncStrategy.encryptBlock(swapPartner, secretKey);
        if (encryptedSwapPartner == null) {
            logger.error(prefix + "Encrypting swap partner failed");
            return null;
        }

//        Save data and overwrite if operation is a write
        byte[] res = block.getData();
        if (op.equals(OperationType.WRITE)) {
            if (recursiveLookup && !lookaheadSetup) {
                Map<Integer, Integer> map = Util.getMapFromByteArray(res);
                map.put(address, Util.byteArrayToLeInt(data));
                block.setData(Util.getByteArrayFromMap(map));
            } else
                block.setData(data);
        }

//        Handle the switch around of the blocks
        BlockLookahead blockToWriteBackToMatrix;
        if (blockFoundInMatrix)
            blockToWriteBackToMatrix = swapPartner;
        else if (blockFoundInAccessStash) {
            blockToWriteBackToMatrix = swapPartner;
//            Remove old version of block
            accessStash = removeFromAccessStash(accessStash, indexOfCurrentAddress);
        } else {
            blockToWriteBackToMatrix = getLookaheadDummyBlock();
            BlockLookahead swapReplacement = new BlockLookahead(swapPartner.getAddress(), swapPartner.getData());
            swapReplacement.setIndex(indexOfCurrentAddress);
            swapStash[swapCount] = swapReplacement;
        }

//        Update position map
//        If the swap partner were a dummy block, the position of the desired address is update twice to hide that fact
        updatePositionMapFailed(swapPartner.getAddress(), getFlatArrayIndex(swapPartner.getIndex()));
        updatePositionMapFailed(block.getAddress(), getFlatArrayIndex(block.getIndex()));

        if (blockInColumn)
            column.set(indexOfCurrentAddress.getRowIndex(), blockToWriteBackToMatrix);

        pickNewFutureSwapPartner(swapStash);
        List<BlockLookahead> blocksFromMaintenance = maintenanceJob(column, accessStash, swapStash);
        if (blocksFromMaintenance == null) {
            logger.error(prefix + "Failed doing maintenance");
            return null;
        }

        List<Integer> addresses = new ArrayList<>();
//        Add addresses for maintenance column
        for (int i = 0; i < matrixHeight; i++)
            addresses.add(getFlatArrayIndex(new Index(i, maintenanceColumnIndex)) + offset);

//        Add addresses for access stash
        for (int i = 0; i < matrixHeight; i++)
            addresses.add(size + i + offset);

//        Add addresses for swap stash
        for (int i = 0; i < matrixHeight; i++)
            addresses.add(size + matrixHeight + i + offset);

        if (!blockInColumn) {
            blocksFromMaintenance.add(blockToWriteBackToMatrix);
            addresses.add(getFlatArrayIndex(indexOfCurrentAddress) + offset);
        }

        List<BlockEncrypted> encryptedBlocks = blockEncStrategy.encryptBlocks(blocksFromMaintenance, secretKey);
        if (encryptedBlocks == null) {
            logger.error(prefix + "Unable to encrypt blocks");
            return null;
        }

        boolean writeStatus = communicationStrategy.writeArray(addresses, encryptedBlocks);
        if (!writeStatus) {
            logger.error(prefix + "Unable to write blocks to server");
            return null;
        }

        accessCounter++;

        if (uploadPositionMap) {
            if (writePositionMapFailed())
                return null;
            positionMap = null;
        }

        return res;
    }

    private List<BlockLookahead> readBlocks(Index indexOfCurrentAddress, int maintenanceColumn, boolean blockInColumn) {
        List<Integer> indices = new ArrayList<>();

        if (!blockInColumn)
            indices.add(getFlatArrayIndex(indexOfCurrentAddress) + offset);

        for (int i = 0; i < matrixHeight; i++)
            indices.add(getFlatArrayIndex(new Index(i, maintenanceColumn)) + offset);

        indices.addAll(getIndicesForAccessStash());
        indices.addAll(getIndicesForSwapStash());

        List<BlockEncrypted> encryptedBlocks = communicationStrategy.readArray(indices);
        if (encryptedBlocks == null) {
            logger.error(prefix + "Unable to read blocks");
            return null;
        }

        List<BlockLookahead> blocks = blockEncStrategy.decryptBlocks(encryptedBlocks, secretKey);
        if (blocks == null) {
            logger.error(prefix + "Unable to decrypt blocks");
            return null;
        }
        return blocks;
    }

    private List<BlockLookahead> maintenanceJob(List<BlockLookahead> column,
                                                Map<Integer, Map<Integer, BlockLookahead>> accessStash,
                                                BlockLookahead[] swapStash) {
        int columnIndex = Math.floorMod(accessCounter, matrixHeight);

//        Move blocks from access stash to column
        Map<Integer, BlockLookahead> map = accessStash.getOrDefault(columnIndex, new HashMap<>());
        for (Map.Entry<Integer, BlockLookahead> entry : map.entrySet()) {
            BlockLookahead blockLookahead = column.get(entry.getKey());
            int address = blockLookahead.getAddress();
            if (!Util.isDummyAddress(address)) {
                logger.error(prefix + "Was suppose to add accessed block to stash at index (" + entry.getKey() + ", " +
                        columnIndex + "), but place were not filled with dummy block");
                return null;
            }
            column.set(entry.getKey(), entry.getValue());
        }

        accessStash.remove(columnIndex);

//        Move blocks from column to swap stash
        for (int i = futureSwapPartners.size() - 1; i >= 0; i--) {
            SwapPartnerData swap = futureSwapPartners.get(i);
            if (swap.getIndex().getColIndex() == columnIndex) {
                int rowIndex = swap.getIndex().getRowIndex();
                BlockLookahead swapPartner = column.get(rowIndex);
//                This test could actually be applied if #blocks = size, test that
//                if (Util.isDummyAddress(swapPartner.getAddress())) {
//                    logger.error("Trying to set a dummy block as swap partner with swap data: " + swap);
//                    return null;
//                }

                swapStash[Math.floorMod(swap.getSwapNumber(), matrixHeight)] = swapPartner;
                futureSwapPartners.remove(i);
                column.set(rowIndex, getLookaheadDummyBlock());
            }
        }

//        Putting the blocks back into a result list
        List<BlockLookahead> res;

//        Column
        res = column;

//        Access stash
        List<BlockLookahead> accessStashList = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, BlockLookahead>> innerMap : accessStash.entrySet()) {
            for (Map.Entry<Integer, BlockLookahead> entry : innerMap.getValue().entrySet()) {
                accessStashList.add(entry.getValue());
            }
        }

        for (int i = 0; i < matrixHeight; i++)
            if (accessStashList.size() <= i)
                accessStashList.add(getLookaheadDummyBlock());

        accessStashList = permutationStrategy.permuteLookaheadBlocks(accessStashList);
        res.addAll(accessStashList);

//        Swap stash
        List<BlockLookahead> swapStashList = new ArrayList<>();
        for (BlockLookahead block : swapStash) {
            if (block == null)
                swapStashList.add(getLookaheadDummyBlock());
            else
                swapStashList.add(block);
        }
        res.addAll(swapStashList);

        return res;
    }

    private void updatePositionMapFailed(int key, int value) {
        positionMap.put(key, value);
    }

    private Map<Integer, Map<Integer, BlockLookahead>> getAccessStash(List<BlockLookahead> blocks, boolean blockInColumn) {
        int beginIndex = matrixHeight;
        if (!blockInColumn) beginIndex++;
        int endIndex = beginIndex + matrixHeight;

        Map<Integer, Map<Integer, BlockLookahead>> res = new HashMap<>();
        for (int i = beginIndex; i < endIndex; i++) {
            res = addToAccessStashMap(res, blocks.get(i));
        }

        return res;
    }

    private Map<Integer, Map<Integer, BlockLookahead>> addToAccessStashMap
            (Map<Integer, Map<Integer, BlockLookahead>> map,
             BlockLookahead block) {
        if (block.getAddress() == 0)
            return map;
        int rowIndex = block.getRowIndex();
        int colIndex = block.getColIndex();
        if (map.containsKey(colIndex)) {
            map.get(colIndex).put(rowIndex, block);
        } else {
            map.put(colIndex, new HashMap<>());
            map.get(colIndex).put(rowIndex, block);
        }
        return map;
    }

    private List<Integer> getIndicesForAccessStash() {
        int beginIndex = size + offset;
        int endIndex = beginIndex + matrixHeight;

        return IntStream.range(beginIndex, endIndex).boxed().collect(Collectors.toList());
    }

    private List<Integer> getIndicesForSwapStash() {
        Integer beginIndex = size + matrixHeight + offset;
        Integer endIndex = beginIndex + matrixHeight;

        return IntStream.range(beginIndex, endIndex).boxed().collect(Collectors.toList());
    }

    private Map<Integer, Map<Integer, BlockLookahead>> removeFromAccessStash
            (Map<Integer, Map<Integer, BlockLookahead>> stash,
             Index index) {
        if (stash.containsKey(index.getColIndex())) {
            Map<Integer, BlockLookahead> map = stash.get(index.getColIndex());
            map.remove(index.getRowIndex());
            if (map.isEmpty())
                stash.remove(index.getColIndex());
        }
        return stash;
    }

    private List<BlockLookahead> getColumn(List<BlockLookahead> blocks, boolean blockInColumn, int maintenanceColumn) {
        int beginIndex = 0;
        if (!blockInColumn) beginIndex++;
        int endIndex = beginIndex + matrixHeight;

        List<BlockLookahead> column = blocks.subList(beginIndex, endIndex);

//        The index might not be part of the dummy data, so it is added for each block
        for (int i = 0; i < matrixHeight; i++)
            column.get(i).setIndex(new Index(i, maintenanceColumn));

        return column;
    }

    private BlockLookahead[] getSwapStash(List<BlockLookahead> blocks, boolean blockInColumn) {
        int beginIndex = matrixHeight * 2;
        if (!blockInColumn) beginIndex++;

        BlockLookahead[] res = new BlockLookahead[matrixHeight];
        for (int i = 0; i < matrixHeight; i++) {
            res[i] = blocks.get(beginIndex + i);
        }
        return res;
    }

    private BlockLookahead findBlockInAccessStash(Map<Integer, Map<Integer, BlockLookahead>> stash, Index index) {
        int colIndex = index.getColIndex();
        int rowIndex = index.getRowIndex();
        if (stash.containsKey(colIndex)) {
            Map<Integer, BlockLookahead> columnMap = stash.get(colIndex);
            BlockLookahead res = columnMap.getOrDefault(rowIndex, null);
            if (res == null || Util.isDummyAddress(res.getAddress())) return null;
            return res;
        }
        return null;
    }

    private Pair<BlockLookahead, Integer> findBlockInSwapStash(BlockLookahead[] stash, int address) {
        for (int i = 0; i < stash.length; i++) {
            BlockLookahead block = stash[i];
            if (block != null && block.getAddress() == address)
                return new ImmutablePair<>(block, i);
        }
        return null;
    }

    private void pickNewFutureSwapPartner(BlockLookahead[] swapStash) {
        List<BlockLookahead> swapStashList = Arrays.asList(swapStash);


        SecureRandom randomness = new SecureRandom();
        boolean futureSwapsContainsIndex = true;
        boolean swapStashContains = true;
        Index index = null;
        while (futureSwapsContainsIndex || swapStashContains) {
            index = new Index(randomness.nextInt(matrixHeight), randomness.nextInt(matrixHeight));
            Index finalIndex = index;
            futureSwapsContainsIndex = futureSwapPartners.stream().anyMatch(f -> f.getIndex().equals(finalIndex));
            swapStashContains = swapStashList.stream().anyMatch(s -> (s != null && s.getIndex().equals(finalIndex)));
        }
        futureSwapPartners.add(new SwapPartnerData(index, accessCounter));
    }

    private int getFlatArrayIndex(Index index) {
        int res = index.getRowIndex();
        res += index.getColIndex() * matrixHeight;
        return res;
    }

    private Index getIndexFromFlatArrayIndex(int flatArrayIndex) {
        int column = flatArrayIndex / matrixHeight;
        int row = flatArrayIndex % matrixHeight;
        return new Index(row, column);
    }

    private List<BlockLookahead> trivialToLookaheadBlocksForSetup(List<BlockTrivial> blocks) {
        List<BlockLookahead> res = new ArrayList<>();
        for (int i = 0; i < matrixHeight; i++) { // Columns
            for (int j = 0; j < matrixHeight; j++) { // Rows
                Index index = new Index(j, i);
                BlockTrivial blockTrivial = blocks.get(getFlatArrayIndex(index));
                res.add(new BlockLookahead(blockTrivial.getAddress(), blockTrivial.getData(), j, i));
                if (blockTrivial.getAddress() != 0)
                    positionMap.put(blockTrivial.getAddress(), getFlatArrayIndex(index));
            }
        }
        return res;
    }

    private BlockLookahead getLookaheadDummyBlock() {
        BlockLookahead blockLookahead = new BlockLookahead(0, new byte[Constants.BLOCK_SIZE]);
        blockLookahead.setIndex(new Index(0, 0));
        return blockLookahead;
    }
}
