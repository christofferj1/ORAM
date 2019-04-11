package oram.lookahead;


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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.*;

import static oram.Constants.INTEGER_BYTE_ARRAY_SIZE;
import static oram.Util.byteArrayToLeInt;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 04-03-2019. <br>
 * Master Thesis 2019 </p>
 * <p>
 * Without keeping the stash locally
 */

public class AccessStrategyLookaheadOld implements AccessStrategy {
    private final SecretKey secretKey;
    private final Logger logger = LogManager.getLogger("log");
    private final int size;
    private final int matrixHeight; // Assumes to be equal to matrix width
    private final CommunicationStrategy communicationStrategy;
    private final EncryptionStrategy encryptionStrategy;
    private final PermutationStrategy permutationStrategy;
    private Map<Integer, Index> positionMap;
    private int accessCounter;
    private List<SwapPartnerData> futureSwapPartners;


    public AccessStrategyLookaheadOld(int size, int matrixHeight, byte[] key, Factory factory) {
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

        logger.info("######### Initialized Lookahead ORAM strategy #########");
        logger.debug("######### Initialized Lookahead ORAM strategy #########");
    }

    @Override
    public boolean setup(List<BlockStandard> blocks) {
//        Fill with dummy blocks
        for (int i = blocks.size(); i < size; i++) {
            blocks.add(new BlockStandard(0, new byte[Constants.BLOCK_SIZE]));
        }

//        Shuffle and convert
        blocks = permutationStrategy.permuteStandardBlocks(blocks);
        List<BlockLookahead> blockLookaheads = standardToLookaheadBlocksForSetup(blocks);

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

        for (int i = 0; i < (size + matrixHeight * 2); i++) {
            if (!communicationStrategy.write(i, encryptedList.get(i))) {
                logger.error("Writing blocks were unsuccessful when initializing the ORAM");
                return false;
            }
        }

        return true;
    }

    @Override
    public byte[] access(OperationType op, int address, byte[] data, boolean recursiveLookup) {
//        Fetch stashes
        Map<Integer, Map<Integer, BlockLookahead>> accessStash = getAccessStash();
        BlockLookahead[] swapStash = getSwapStash();

        Index indexOfCurrentAddress = positionMap.get(address);

//        Fetch block from either matrix, access stash or swap stash
        BlockLookahead block = fetchBlockFromMatrix(indexOfCurrentAddress);
        boolean blockFoundInMatrix = true;
        boolean blockFoundInAccessStash = true;
        int swapCount = 0;
        if (Util.isDummyAddress(block.getAddress())) {
            blockFoundInMatrix = false;
            block = findBlockInAccessStash(accessStash, indexOfCurrentAddress);
            if (block == null) {
                blockFoundInAccessStash = false;
                Pair<BlockLookahead, Integer> pair = findBlockInSwapStash(swapStash, address);
                if (pair == null) {
                    logger.error("Unable to locate block, address: " + address);
                    return null;
                } else {
                    block = pair.getKey();
                    swapCount = pair.getValue();
                    logger.info("Block found in swap stash: " + block.toStringShort());
                }
            } else
                logger.info("Block found in access stash: " + block.toStringShort());
        } else
            logger.info("Block found in matrix: " + block.toStringShort());

//        Get swap partner
        int accessCounterMod = Math.floorMod(accessCounter, matrixHeight);
        BlockLookahead swapPartner = swapStash[accessCounterMod];
        swapStash[accessCounterMod] = null;

//        Set index to index of swap partner and add to access stash
        block.setIndex(swapPartner.getIndex());
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
        positionMap.put(swapPartner.getAddress(), swapPartner.getIndex());
        positionMap.put(block.getAddress(), block.getIndex());

//        Handle the switch around of the blocks
        int flatArrayIndex = getFlatArrayIndex(indexOfCurrentAddress);
        BlockEncrypted blockToWriteBackToMatrix;
        if (blockFoundInMatrix)
            blockToWriteBackToMatrix = encryptedSwapPartner;
        else if (blockFoundInAccessStash) {
            blockToWriteBackToMatrix = encryptedSwapPartner;
//            Remove old version of block
            accessStash = removeFromAccessStash(accessStash, indexOfCurrentAddress);
        } else {
            blockToWriteBackToMatrix = encryptBlock(getLookaheadDummyBlock());
            if (blockToWriteBackToMatrix == null) {
                logger.error("Unable to encrypt dummy block");
                return null;
            }
            BlockLookahead swapReplacement = new BlockLookahead(swapPartner.getAddress(), swapPartner.getData());
            swapReplacement.setIndex(indexOfCurrentAddress);
            swapStash[swapCount] = swapReplacement;
            positionMap.put(swapReplacement.getAddress(), swapReplacement.getIndex());
        }

//        Write block back to the matrix
        if (!communicationStrategy.write(flatArrayIndex, blockToWriteBackToMatrix)) {
            logger.error("Unable to write swap partner to communicationStrategy: \n" + swapPartner.toString());
            return null;
        }

        pickNewFutureSwapPartner(swapStash);
        if (!maintenanceJob(accessStash, swapStash)) {
            logger.error("Failed doing maintenance");
            return null;
        }

        accessCounter++;

        return res;
    }

    boolean maintenanceJob(Map<Integer, Map<Integer, BlockLookahead>> accessStash, BlockLookahead[] swapStash) {
        int columnIndex = Math.floorMod(accessCounter, matrixHeight);
        List<BlockLookahead> column = new ArrayList<>();

//        Retrieve column from matrix
        for (int i = 0; i < matrixHeight; i++) {
            BlockEncrypted encryptedBlock = communicationStrategy.read(getFlatArrayIndex(new Index(i, columnIndex)));
            if (encryptedBlock == null) {
                logger.error("Unable to read block with index (" + i + ", " + columnIndex + ") from communicationStrategy");
                return false;
            }
            BlockLookahead block = decryptToLookaheadBlock(encryptedBlock);
            column.add(block);
        }

//        Move blocks from access stash to column
        Map<Integer, BlockLookahead> map = accessStash.getOrDefault(columnIndex, new HashMap<>());
        for (Map.Entry<Integer, BlockLookahead> entry : map.entrySet()) {
            if (!Util.isDummyAddress(column.get(entry.getKey()).getAddress())) {
                logger.error("Was suppose to add accessed block to stash at index (" + entry.getKey() + ", " +
                        columnIndex + "), but place were not filled with dummy block");
                return false;
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
//                if (Util.isDummyAddress(swapPartner.getAddress())) { // TODO: this could actually be applied if #blocks = size, test that
//                    logger.error("Trying to set a dummy block as swap partner with swap data: " + swap);
//                    return false;
//                }
                swapStash[Math.floorMod(swap.getSwapNumber(), matrixHeight)] = swapPartner;
                futureSwapPartners.remove(i);
                column.set(rowIndex, getLookaheadDummyBlock());
            }
        }

//        Write back column
        List<BlockEncrypted> encryptedBlocks = encryptBlocks(column);
        if (encryptedBlocks.size() != matrixHeight) {
            logger.error("Wrong number of encrypted blocks to write back: " + encryptedBlocks.size());
            return false;
        }
        for (int i = 0; i < matrixHeight; i++) {
            Index index = new Index(i, columnIndex);
            if (!communicationStrategy.write(getFlatArrayIndex(index), encryptedBlocks.get(i))) {
                logger.error("Unable to write block to column with index: " + index);
                return false;
            }
        }

//        Write back access stash
        encryptedBlocks = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, BlockLookahead>> innerMap : accessStash.entrySet()) {
            for (Map.Entry<Integer, BlockLookahead> entry : innerMap.getValue().entrySet()) {
                BlockEncrypted block = encryptBlock(entry.getValue());
                if (block == null) {
                    logger.error("Unable to encrypt block");
                    return false;
                }
                encryptedBlocks.add(block);
            }
        }
        for (int i = 0; i < matrixHeight; i++) {
            int index = size + i;
            BlockEncrypted block;
            if (encryptedBlocks.size() <= i)
                block = encryptBlock(getLookaheadDummyBlock());
            else
                block = encryptedBlocks.get(i);

            if (block == null) {
                logger.error("Unable to encrypt block");
                return false;
            }

            if (!communicationStrategy.write(index, block)) {
                logger.error("Unable to write block to access stash with index: " + index);
                return false;
            }
        }

//        Write back swap stash
        encryptedBlocks = new ArrayList<>();
        for (BlockLookahead block : swapStash) {
            BlockEncrypted encryptedBlock;
            if (block == null)
                encryptedBlock = encryptBlock(getLookaheadDummyBlock());
            else
                encryptedBlock = encryptBlock(block);

            if (encryptedBlock == null) {
                logger.error("Unable to encrypt block");
                return false;
            }
            encryptedBlocks.add(encryptedBlock);
        }
        for (int i = 0; i < matrixHeight; i++) {
            int index = size + matrixHeight + i;
            if (!communicationStrategy.write(index, encryptedBlocks.get(i))) {
                logger.error("Unable to write block to swap stash with index: " + index);
                return false;
            }
        }

        return true;
    }

    Map<Integer, Map<Integer, BlockLookahead>> getAccessStash() {
        int beginIndex = size;
        int endIndex = beginIndex + matrixHeight;

        Map<Integer, Map<Integer, BlockLookahead>> res = new HashMap<>();
        for (int i = beginIndex; i < endIndex; i++) {
            BlockEncrypted blockRead = communicationStrategy.read(i);
            BlockLookahead blockLookahead = decryptToLookaheadBlock(blockRead);
            res = addToAccessStashMap(res, blockLookahead);
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

    Map<Integer, Map<Integer, BlockLookahead>> removeFromAccessStash
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

    BlockLookahead[] getSwapStash() {
        int flattenedArrayOffSet = size + matrixHeight;

        BlockLookahead[] res = new BlockLookahead[matrixHeight];
        for (int i = 0; i < matrixHeight; i++) {
            res[i] = decryptToLookaheadBlock(communicationStrategy.read(i + flattenedArrayOffSet));
        }
        return res;
    }

    BlockLookahead fetchBlockFromMatrix(Index index) {
        int serverIndex = getFlatArrayIndex(index);

        return decryptToLookaheadBlock(communicationStrategy.read(serverIndex));
    }

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

    //    TODO: check for null the correct places
    public BlockLookahead decryptToLookaheadBlock(BlockEncrypted blockEncrypted) {
        byte[] encryptedDataFull = blockEncrypted.getData();
        int encryptedDataFullLength = encryptedDataFull.length;
        int endOfDataIndex = encryptedDataFullLength - Constants.AES_BLOCK_SIZE * 2;
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

    List<BlockLookahead> standardToLookaheadBlocksForSetup(List<BlockStandard> blocks) {
        List<BlockLookahead> res = new ArrayList<>();
        for (int i = 0; i < matrixHeight; i++) { // Columns
            for (int j = 0; j < matrixHeight; j++) { // Rows
                Index index = new Index(j, i);
                BlockStandard blockStandard = blocks.get(getFlatArrayIndex(index));
                res.add(new BlockLookahead(blockStandard.getAddress(), blockStandard.getData(), j, i));
                if (blockStandard.getAddress() != 0)
                    positionMap.put(blockStandard.getAddress(), index);
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
