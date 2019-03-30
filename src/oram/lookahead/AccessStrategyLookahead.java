package oram.lookahead;


import javafx.util.Pair;
import oram.AccessStrategy;
import oram.Constants;
import oram.OperationType;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockLookahead;
import oram.block.BlockStandard;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.factory.Factory;
import oram.permutation.PermutationStrategy;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static oram.Constants.INTEGER_BYTE_ARRAY_SIZE;
import static oram.Util.byteArrayToLeInt;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 04-03-2019. <br>
 * Master Thesis 2019 </p>
 * <p>
 * Without keeping the stash locally
 */

public class AccessStrategyLookahead implements AccessStrategy {
    private final SecretKey secretKey;
    private final Logger logger = LogManager.getLogger("log");
    private final int size;
    private final int matrixHeight; // Assumes to be equal to matrix width
    private final CommunicationStrategy communicationStrategy;
    private final EncryptionStrategy encryptionStrategy;
    private final PermutationStrategy permutationStrategy;
    public List<Integer> knownDummyAddresses;
    private Map<Integer, Integer> positionMap;
    private int accessCounter;
    private List<SwapPartnerData> futureSwapPartners;


    public AccessStrategyLookahead(int size, int matrixHeight, byte[] key, Factory factory) {
        this.size = size;
        this.matrixHeight = matrixHeight;
        this.communicationStrategy = factory.getCommunicationStrategy();
        this.encryptionStrategy = factory.getEncryptionStrategy();
        this.secretKey = encryptionStrategy.generateSecretKey(key);
        this.permutationStrategy = factory.getPermutationStrategy();
        if (!(size == matrixHeight * matrixHeight))
            logger.error("Size of matrix is wrong");
        accessCounter = 0;
        futureSwapPartners = new ArrayList<>();
        positionMap = new HashMap<>();
        knownDummyAddresses = new ArrayList<>();

        logger.info("######### Initialized Lookahead ORAM strategy #########");
        logger.debug("######### Initialized Lookahead ORAM strategy #########");
    }

    @Override
    public boolean setup(List<BlockStandard> block) {
        List<Integer> addresses = new ArrayList<>();
        List<BlockLookahead> lookaheadBlocks = new ArrayList<>();
        for (int i = 0; i < size + matrixHeight; i++) {
            addresses.add(i);
            lookaheadBlocks.add(getLookaheadDummyBlock());
        }

        //        Pick swap partners
        SecureRandom randomness = new SecureRandom();
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

            BlockLookahead lookaheadDummyBlock = getLookaheadDummyBlock();
            lookaheadDummyBlock.setIndex(index);
            addresses.add(size + matrixHeight + i);
            lookaheadBlocks.add(lookaheadDummyBlock);
        }
        futureSwapPartners = new ArrayList<>();

        knownDummyAddresses = IntStream.range(0, size).boxed().collect(Collectors.toList());

        return communicationStrategy.writeArray(addresses, encryptBlocks(lookaheadBlocks));
    }

    public boolean setupOld(List<BlockStandard> blocks) {
//        Fill with dummy blocks
        for (int i = blocks.size(); i < size; i++) {
            blocks.add(new BlockStandard(0, new byte[Constants.BLOCK_SIZE]));
        }

//        Shuffle and convert
        blocks = permutationStrategy.permuteStandardBlocks(blocks);
        List<BlockLookahead> blockLookaheads = standardToLookaheadBlocksForSetup(blocks);
        for (int i = 0; i < size; i++) {
            if (Util.isDummyAddress(blockLookaheads.get(i).getAddress()))
                knownDummyAddresses.add(i);
        }

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
            int flatArrayIndex = getFlatArrayIndex(index);
            swapPartners.add(blockLookaheads.get(flatArrayIndex));
        }

//        Add blocks to the right place in the flattened array
//        First the matrix
        List<BlockLookahead> res = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            BlockLookahead block = blockLookaheads.get(i);
            Index index = block.getIndex();
            boolean isSwapPartner = futureSwapPartners.stream().anyMatch(s -> s.getIndex().equals(index));
            if (isSwapPartner)
                res.add(getLookaheadDummyBlock());
            else
                res.add(blockLookaheads.get(i));
        }
        futureSwapPartners = new ArrayList<>();

//        Then the access swap
        for (int i = size; i < size + matrixHeight; i++)
            res.add(getLookaheadDummyBlock());

//        At last the swap stash
        for (int i = (size + matrixHeight); i < (size + matrixHeight * 2); i++)
            res.add(swapPartners.get(i - (size + matrixHeight)));


//        Encrypt and write blocks to server
        List<BlockEncrypted> encryptedList = encryptBlocks(res);
        if (encryptedList.isEmpty()) {
            logger.error("Unable to decrypt when initializing the ORAM");
            return false;
        }

        List<Integer> addresses = new ArrayList<>();
        for (int i = 0; i < encryptedList.size(); i++)
            addresses.add(i);

        boolean writeSuccess = communicationStrategy.writeArray(addresses, encryptedList);
        if (!writeSuccess) {
            logger.error("Writing blocks were unsuccessful when initializing the ORAM");
            return false;
        }

        return true;
    }

    @Override
    public byte[] access(OperationType op, int address, byte[] data) {
        SecureRandom randomness = new SecureRandom();

//        Find position
        Integer position = positionMap.getOrDefault(address, null);
        if (position == null) {
            if (op.equals(OperationType.WRITE)) {
                position = knownDummyAddresses.get(randomness.nextInt(knownDummyAddresses.size()));
            } else {
                logger.error("Tried to read an address never written to");
                return null;
            }
        }

//        Find index from position and maintenance column
        Index indexOfCurrentAddress = getIndexFromFlatArrayIndex(position);
        int maintenanceColumnIndex = Math.floorMod(accessCounter, matrixHeight);

        logger.info("Access op: " + op.toString() + ", address: " + address + ", index: ("
                + indexOfCurrentAddress.getRowIndex() + ", " + indexOfCurrentAddress.getColIndex() +
                ") (flat: " + position + "), maintenance column: " + maintenanceColumnIndex);

        System.out.println("Access op: " + op.toString() + ", address: " + address + ", index: ("
                + indexOfCurrentAddress.getRowIndex() + ", " + indexOfCurrentAddress.getColIndex() +
                ") (flat: " + position + "), maintenance column: " + maintenanceColumnIndex);

//        This tells if the block we fetch is in the column used for maintenance
        boolean blockInColumn = indexOfCurrentAddress.getColIndex() == maintenanceColumnIndex;

        List<BlockLookahead> blocks = readBlocks(indexOfCurrentAddress, maintenanceColumnIndex, blockInColumn);
        if (blocks == null) {
            logger.error("Blocks read from server were null");
            return null;
        }

//        If the block is found in the column, we fetch one less
        int numberOfBlocksToFetch = blockInColumn ? matrixHeight * 3 : matrixHeight * 3 + 1;
        if (blocks.size() != numberOfBlocksToFetch) {
            logger.error("The number of blocks read from the server: " + blocks.size() + " should be: "
                    + numberOfBlocksToFetch);
        }

//        Fetch column and stashes from fetched blocks
        List<BlockLookahead> column = getColumn(blocks, blockInColumn);
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
        boolean blockFoundInSwapStash = true;
        int swapCount = 0;
        if (Util.isDummyAddress(block.getAddress())) {
            blockFoundInMatrix = false;
            block = findBlockInAccessStash(accessStash, indexOfCurrentAddress);
            if (block == null) {
                blockFoundInAccessStash = false;
                Pair<BlockLookahead, Integer> pair = findBlockInSwapStash(swapStash, indexOfCurrentAddress);
                if (pair == null) {
                    block = new BlockLookahead(address, Constants.DUMMY_RESPONSE.getBytes()); // Index is set below
                    blockFoundInSwapStash = false;
                    logger.error("Did not find blog");
                    System.out.println("Did not find blog");
                } else {
                    block = pair.getKey();
                    swapCount = pair.getValue();
                    if (Util.isDummyAddress(block.getAddress()))
                        block = new BlockLookahead(address, Constants.DUMMY_RESPONSE.getBytes());
                    logger.info("Block found in swap stash: " + block.toStringShort());
                    System.out.println("Block found in swap stash: " + block.toStringShort());
                }
            } else {
                logger.info("Block found in access stash: " + block.toStringShort());
                System.out.println("Block found in access stash: " + block.toStringShort());
            }
        } else {
            logger.info("Block found in matrix: " + block.toStringShort());
            System.out.println("Block found in matrix: " + block.toStringShort());
        }

//        Get swap partner
        BlockLookahead swapPartner = swapStash[maintenanceColumnIndex];
        Index swapIndex = swapPartner.getIndex();
        swapStash[maintenanceColumnIndex] = null;
        System.out.println("Chosen swap partner (flat: " + getFlatArrayIndex(swapIndex) + "): " + swapPartner.toStringShort());

        System.out.println("Update known dummies");
        System.out.println("  Before:");
        Collections.sort(knownDummyAddresses);
        System.out.println("    " + Arrays.toString(knownDummyAddresses.toArray()));
        if (Util.isDummyAddress(swapPartner.getAddress())) {
            if (!knownDummyAddresses.contains(position)) {
                knownDummyAddresses.add(position);
            }
        } else {
            int index = knownDummyAddresses.indexOf(position);
            if (index >= 0) {
//                knownDummyAddresses.remove(index);
                knownDummyAddresses.retainAll(Collections.singletonList(position));
            }
        }

        int index = knownDummyAddresses.indexOf(getFlatArrayIndex(swapIndex));
        if (index >= 0) {
//            knownDummyAddresses.remove(index);
            knownDummyAddresses.removeAll(Collections.singletonList(getFlatArrayIndex(swapIndex)));
        }

        Collections.sort(knownDummyAddresses);
        System.out.println("  After:");
        System.out.println("    " + Arrays.toString(knownDummyAddresses.toArray()));


//        Set index to index of swap partner and add to access stash
        block.setIndex(swapIndex);
        accessStash = addToAccessStashMap(accessStash, block);

//        Update swap partner index and encrypt it
        swapPartner.setIndex(indexOfCurrentAddress);
        BlockEncrypted encryptedSwapPartner = encryptBlock(swapPartner);
        if (encryptedSwapPartner == null) {
            logger.error("Encrypting swap partner failed");
            return null;
        }

//        Save data and overwrite if operation is a write
        byte[] res = block.getData();
        if (op.equals(OperationType.WRITE)) {block.setData(data);}

//        Update position map
        positionMap.put(swapPartner.getAddress(), getFlatArrayIndex(swapPartner.getIndex()));
        positionMap.put(block.getAddress(), getFlatArrayIndex(block.getIndex()));

//        Handle the switch around of the blocks
        BlockLookahead blockToWriteBackToMatrix;
        if (blockFoundInMatrix)
            blockToWriteBackToMatrix = swapPartner;
        else if (blockFoundInAccessStash) {
            blockToWriteBackToMatrix = swapPartner;
//            Remove old version of block
            accessStash = removeFromAccessStash(accessStash, indexOfCurrentAddress);
        } else if (blockFoundInSwapStash) {
            blockToWriteBackToMatrix = getLookaheadDummyBlock();
            BlockLookahead swapReplacement = new BlockLookahead(swapPartner.getAddress(), swapPartner.getData());
            swapReplacement.setIndex(indexOfCurrentAddress);
            swapStash[swapCount] = swapReplacement;
            positionMap.put(swapReplacement.getAddress(), getFlatArrayIndex(swapReplacement.getIndex()));
        } else
            blockToWriteBackToMatrix = swapPartner;

//        TODO: if block in column, update that before parsing the column along to the maintenance job
        if (blockInColumn)
            column.set(indexOfCurrentAddress.getRowIndex(), blockToWriteBackToMatrix);

        pickNewFutureSwapPartner(swapStash);
        List<BlockLookahead> blocksFromMaintenance = maintenanceJob(column, swapStash, accessStash);
        if (blocksFromMaintenance == null) {
            logger.error("Failed doing maintenance");
            return null;
        }

        List<Integer> addresses = new ArrayList<>();
//        Add addresses for maintenance column
        for (int i = 0; i < matrixHeight; i++)
            addresses.add(getFlatArrayIndex(new Index(i, maintenanceColumnIndex)));

//        Add addresses for access stash
        for (int i = 0; i < matrixHeight; i++)
            addresses.add(size + i);

//        Add addresses for swap stash
        for (int i = 0; i < matrixHeight; i++)
            addresses.add(size + matrixHeight + i);

        if (!blockInColumn) {
            blocksFromMaintenance.add(blockToWriteBackToMatrix);
            addresses.add(getFlatArrayIndex(indexOfCurrentAddress));
        }

        List<BlockEncrypted> encryptedBlocks = encryptBlocks(blocksFromMaintenance);
        if (encryptedBlocks == null) {
            logger.error("Unable to encrypt blocks");
            return null;
        }

//        System.out.println("Addresses size: " + addresses.size() + ", blocks size: " + blocksFromMaintenance.size());
//        System.out.println("Blocks to write");
//        for (int i = 0; i < blocksFromMaintenance.size(); i++)
//            System.out.println("  Index: " + addresses.get(i) + ", " + blocksFromMaintenance.get(i).toStringShort());

        boolean writeStatus = communicationStrategy.writeArray(addresses, encryptedBlocks);
        if (!writeStatus) {
            logger.error("Unable to write blocks to server");
            return null;
        }

        accessCounter++;

        System.out.println("Position map");
        for (Map.Entry e : positionMap.entrySet())
            System.out.print(e.getKey() + " -> " + e.getValue() + ", ");
        System.out.println(" ");

        return res;
    }

    private List<BlockLookahead> readBlocks(Index indexOfCurrentAddress, int maintenanceColumn, boolean blockInColumn) {
        List<Integer> indices = new ArrayList<>();

        if (!blockInColumn)
            indices.add(getFlatArrayIndex(indexOfCurrentAddress));

        for (int i = 0; i < matrixHeight; i++)
            indices.add(getFlatArrayIndex(new Index(i, maintenanceColumn)));

        indices.addAll(getIndicesForAccessStash());
        indices.addAll(getIndicesForSwapStash());

        List<BlockEncrypted> encryptedBlocks = communicationStrategy.readArray(indices);
        if (encryptedBlocks == null) {
            logger.error("Unable to read blocks");
            return null;
        }

        List<BlockLookahead> blocks = decryptLookaheadBlocks(encryptedBlocks);
        if (blocks == null) {
            logger.error("Unable to decrypt blocks");
            return null;
        }
        return blocks;
    }

    private List<BlockLookahead> maintenanceJob(List<BlockLookahead> column, BlockLookahead[] swapStash,
                                                Map<Integer, Map<Integer, BlockLookahead>> accessStash) {
        int columnIndex = Math.floorMod(accessCounter, matrixHeight);
//        List<BlockLookahead> column = new ArrayList<>();

////        Retrieve column from matrix
//        for (int i = 0; i < matrixHeight; i++) {
//            BlockEncrypted encryptedBlock = communicationStrategy.read(getFlatArrayIndex(new Index(i, columnIndex)));
//            if (encryptedBlock == null) {
//                logger.error("Unable to read block with index (" + i + ", " + columnIndex + ") from communicationStrategy");
//                return false;
//            }
//            BlockLookahead block = decryptToLookaheadBlock(encryptedBlock);
//            column.add(block);
//        }

//        Move blocks from access stash to column
        Map<Integer, BlockLookahead> map = accessStash.getOrDefault(columnIndex, new HashMap<>());
        System.out.println("Access stash column");
        for (Map.Entry<Integer, BlockLookahead> entry : map.entrySet())
            System.out.println("    " + entry.getKey() + " -> " + entry.getValue());

        for (Map.Entry<Integer, BlockLookahead> entry : map.entrySet()) {
            if (!Util.isDummyAddress(column.get(entry.getKey()).getAddress())) {
                logger.error("Was suppose to add accessed block to stash at index (" + entry.getKey() + ", " +
                        columnIndex + "), but place were not filled with dummy block");
                return null;
            }
            column.set(entry.getKey(), entry.getValue());
        }

        accessStash.remove(columnIndex);

//        Move blocks from column to swap stash
        System.out.println("Move blocks from column to swap, column: " + columnIndex);
        System.out.println("    Current swap stash:");
        for (BlockLookahead b : swapStash)
            System.out.println("      " + (b != null ? b.toStringShort() : null));
        System.out.println("    Current column");
        for (BlockLookahead b : column)
            System.out.println("      " + (b != null ? b.toStringShort() : null));
        System.out.println("    Future swap partners");
        for (SwapPartnerData s : futureSwapPartners)
            System.out.println("      " + s.toString());
        System.out.println("      --------------------------");

        for (int i = futureSwapPartners.size() - 1; i >= 0; i--) {
            SwapPartnerData swap = futureSwapPartners.get(i);
//            System.out.println("  Future swap partner: " + swap.toString());
            if (swap.getIndex().getColIndex() == columnIndex) {
                int rowIndex = swap.getIndex().getRowIndex();
//                System.out.println("    Row index: " + rowIndex);
                BlockLookahead swapPartner = column.get(rowIndex);
                System.out.println("    Swap partner: " + swapPartner.toStringShort());
                swapPartner.setIndex(new Index(rowIndex, columnIndex));

//                This could actually be applied if #blocks = size, test that
//                if (Util.isDummyAddress(swapPartner.getAddress())) {
//                    logger.error("Trying to set a dummy block as swap partner with swap data: " + swap);
//                    return null;
//                }

                swapStash[Math.floorMod(swap.getSwapNumber(), matrixHeight)] = swapPartner;
                futureSwapPartners.remove(i);
                column.set(rowIndex, getLookaheadDummyBlock());

            }
        }
        System.out.println("      --------------------------");
        System.out.println("    Current swap stash:");
        for (BlockLookahead b : swapStash)
            System.out.println("      " + (b != null ? b.toStringShort() : null));
        System.out.println("    Current column");
        for (BlockLookahead b : column)
            System.out.println("      " + (b != null ? b.toStringShort() : null));

//        Putting the blocks back into a result list
        List<BlockLookahead> res;

//        Column
        res = column;

//        System.out.println("Blocks returned from maintenance job, size: " + res.size());
//        for (BlockLookahead b : res)
//            System.out.println("    " + b.toStringShort());

//        Access stash
        List<BlockLookahead> accessStashList = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, BlockLookahead>> innerMap : accessStash.entrySet()) {
            for (Map.Entry<Integer, BlockLookahead> entry : innerMap.getValue().entrySet()) {
                accessStashList.add(entry.getValue());
            }
        }

        for (int i = 0; i < matrixHeight; i++) {
//            System.out.println("Adding dummies to access stash, size: " + accessStashList.size());
            if (accessStashList.size() <= i) {
                accessStashList.add(getLookaheadDummyBlock());
//                System.out.println("    Adding one for index: " + i);
            }
        }
        accessStashList = permutationStrategy.permuteLookaheadBlocks(accessStashList);
        res.addAll(accessStashList);

//        System.out.println("Blocks returned from maintenance job, size: " + res.size());
//        for (BlockLookahead b : res)
//            System.out.println("    " + b.toStringShort());

//        Swap stash
        List<BlockLookahead> swapStashList = new ArrayList<>();
        for (BlockLookahead block : swapStash) {
            if (block == null)
                swapStashList.add(getLookaheadDummyBlock());
            else
                swapStashList.add(block);
        }
        res.addAll(swapStashList);

//        System.out.println("Blocks returned from maintenance job, size: " + res.size());
//        for (BlockLookahead b : res)
//            System.out.println("    " + b.toStringShort());

        return res;
    }

    Map<Integer, Map<Integer, BlockLookahead>> getAccessStash(List<BlockLookahead> blocks, boolean blockInColumn) {
        int beginIndex = matrixHeight;
        if (!blockInColumn) beginIndex++;
        int endIndex = beginIndex + matrixHeight;

        Map<Integer, Map<Integer, BlockLookahead>> res = new HashMap<>();
        for (int i = beginIndex; i < endIndex; i++) {
            res = addToAccessStashMap(res, blocks.get(i));
        }

        return res;
    }

    Map<Integer, Map<Integer, BlockLookahead>> addToAccessStashMap
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
        int beginIndex = size;
        int endIndex = beginIndex + matrixHeight;

        return IntStream.range(beginIndex, endIndex).boxed().collect(Collectors.toList());
    }

    private List<Integer> getIndicesForSwapStash() {
        Integer beginIndex = size + matrixHeight;
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

    private List<BlockLookahead> getColumn(List<BlockLookahead> blocks, boolean blockInColumn) {
        int beginIndex = 0;
        if (!blockInColumn) beginIndex++;
        int endIndex = beginIndex + matrixHeight;

        return blocks.subList(beginIndex, endIndex);
    }

    BlockLookahead[] getSwapStash(List<BlockLookahead> blocks, boolean blockInColumn) {
        int beginIndex = matrixHeight * 2;
        if (!blockInColumn) beginIndex++;

        BlockLookahead[] res = new BlockLookahead[matrixHeight];
        for (int i = 0; i < matrixHeight; i++) {
            res[i] = blocks.get(beginIndex + i);
        }
        return res;
    }

    /**
     * Finds the block in the stash, if it has been put there. This is filled with dummy blocks, so they are simply
     * filtered out.
     */
    BlockLookahead findBlockInAccessStash(Map<Integer, Map<Integer, BlockLookahead>> stash, Index index) {
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

    /**
     * Find block from swap based on index, as we during a write access might to overwrite a dummy block in swap stash
     */
    private Pair<BlockLookahead, Integer> findBlockInSwapStash(BlockLookahead[] stash, Index index) {
        for (int i = 0; i < stash.length; i++) {
            BlockLookahead block = stash[i];
            if (block != null && block.getIndex().equals(index))
                return new Pair<>(block, i);
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

    public List<BlockLookahead> decryptLookaheadBlocks(List<BlockEncrypted> encryptedBlocks) {
        List<BlockLookahead> res = new ArrayList<>();
        for (BlockEncrypted b : encryptedBlocks) {
            BlockLookahead block = decryptToLookaheadBlock(b);
            if (block == null) {
                logger.error("Unable to decrypt block");
                return null;
            }
            res.add(block);
        }
        return res;
    }

    //    TODO: check for null the correct places
    public BlockLookahead decryptToLookaheadBlock(BlockEncrypted blockEncrypted) {
        byte[] encryptedDataFull = blockEncrypted.getData();
        int encryptedDataFullLength = encryptedDataFull.length;
        int endOfDataIndex = encryptedDataFullLength - Constants.BLOCK_SIZE * 2;
        byte[] encryptedData = Arrays.copyOfRange(encryptedDataFull, 0, endOfDataIndex);
        byte[] encryptedIndex = Arrays.copyOfRange(encryptedDataFull, endOfDataIndex, encryptedDataFullLength);
        byte[] data = encryptionStrategy.decrypt(encryptedData, secretKey);
        byte[] indices = encryptionStrategy.decrypt(encryptedIndex, secretKey);
        if (data == null) {
            logger.info("Tried to turn an encrypted block with value = null into a Lookahead block");
            return null;
        }

        byte[] addressBytes = encryptionStrategy.decrypt(blockEncrypted.getAddress(), secretKey);

        int address = byteArrayToLeInt(addressBytes);

        int rowDataIndex = 0;
        int colDataIndex = INTEGER_BYTE_ARRAY_SIZE;
        byte[] rowIndexBytes = Arrays.copyOfRange(indices, rowDataIndex, colDataIndex);
        byte[] colIndexBytes = Arrays.copyOfRange(indices, colDataIndex, INTEGER_BYTE_ARRAY_SIZE * 2);

        BlockLookahead blockLookahead = new BlockLookahead();
        blockLookahead.setAddress(address);
        blockLookahead.setData(data);
        blockLookahead.setRowIndex(byteArrayToLeInt(rowIndexBytes));
        blockLookahead.setColIndex(byteArrayToLeInt(colIndexBytes));
        return blockLookahead;
    }

    BlockEncrypted encryptBlock(BlockLookahead block) {
        List<BlockEncrypted> encryptedList = encryptBlocks(Collections.singletonList(block));
        if (encryptedList.isEmpty())
            return null;
        else
            return encryptedList.get(0);
    }

    List<BlockEncrypted> encryptBlocks(List<BlockLookahead> blockLookaheads) {
        List<BlockEncrypted> res = new ArrayList<>();
        for (BlockLookahead block : blockLookaheads) {
            if (block == null) {
                res.add(null);
                continue;
            }
            byte[] rowIndexBytes = Util.leIntToByteArray(block.getRowIndex());
            byte[] colIndexBytes = Util.leIntToByteArray(block.getColIndex());
            byte[] addressBytes = Util.leIntToByteArray(block.getAddress());
            byte[] encryptedAddress = encryptionStrategy.encrypt(addressBytes, secretKey);
            byte[] encryptedData = encryptionStrategy.encrypt(block.getData(), secretKey);
            byte[] encryptedIndex = encryptionStrategy.encrypt(ArrayUtils.addAll(rowIndexBytes, colIndexBytes),
                    secretKey);

            if (encryptedAddress == null || encryptedData == null || encryptedIndex == null) {
                logger.error("Unable to encrypt block: " + block.toStringShort());
                return new ArrayList<>();
            }

            byte[] encryptedDataPlus = ArrayUtils.addAll(encryptedData, encryptedIndex);

            res.add(new BlockEncrypted(encryptedAddress, encryptedDataPlus));
        }
        return res;
    }

    int getFlatArrayIndex(Index index) {
        int res = index.getRowIndex();
        res += index.getColIndex() * matrixHeight;
        return res;
    }

    Index getIndexFromFlatArrayIndex(int flatArrayIndex) {
        int column = flatArrayIndex / matrixHeight;
        int row = flatArrayIndex % matrixHeight;
        return new Index(row, column);
    }

    List<BlockLookahead> standardToLookaheadBlocksForSetup(List<BlockStandard> blocks) {
        List<BlockLookahead> res = new ArrayList<>();
        for (int i = 0; i < matrixHeight; i++) { // Columns
            for (int j = 0; j < matrixHeight; j++) { // Rows
                Index index = new Index(j, i);
                BlockStandard blockStandard = blocks.get(getFlatArrayIndex(index));
                res.add(new BlockLookahead(blockStandard.getAddress(), blockStandard.getData(), j, i));
                if (blockStandard.getAddress() != 0)
                    positionMap.put(blockStandard.getAddress(), getFlatArrayIndex(index));
            }
        }
        return res;
    }

    BlockLookahead getLookaheadDummyBlock() {
        BlockLookahead blockLookahead = new BlockLookahead(0, new byte[Constants.BLOCK_SIZE]);
        blockLookahead.setIndex(new Index(0, 0));
        return blockLookahead;
    }
}
