package oram.lookahead;


import oram.*;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.factory.Factory;
import oram.path.BlockStandard;
import oram.permutation.PermutationStrategy;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.*;

import static oram.Constants.INTEGER_BYTE_ARRAY_SIZE;
import static oram.Util.byteArrayToLeInt;
import static oram.Util.getEncryptedDummy;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 04-03-2019. <br>
 * Master Thesis 2019 </p>
 * <p>
 * Without keeping the stash locally
 */

public class AccessStrategyLookahead implements AccessStrategy {
    private final Logger logger = LogManager.getLogger("log");
    private final int size;
    private final int matrixHeight; // Assumes to be equal to matrix width
    private final SecretKey secretKey;
    private final CommunicationStrategy communicationStrategy;
    private final EncryptionStrategy encryptionStrategy;
    private final PermutationStrategy permutationStrategy;
    private Map<Integer, Index> positionMap;
    private int accessCounter;
    private List<SwapPartnerData> futureSwapPartners;


    AccessStrategyLookahead(int size, int matrixHeight, byte[] key, Factory factory) {
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
    }

    boolean setup(List<BlockStandard> blocks) {
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
        List<BlockLookahead> res = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            BlockLookahead block = blockLookaheads.get(i);
            Index index = block.getIndex();
            boolean isSwapPartner = futureSwapPartners.stream().anyMatch(s -> s.getIndex().equals(index));
            if (isSwapPartner)
                res.add(new BlockLookahead(0, new byte[Constants.BLOCK_SIZE]));
            else
                res.add(blockLookaheads.get(i));
        }
        futureSwapPartners = new ArrayList<>();

        for (int i = size; i < size + matrixHeight; i++)
            res.add(new BlockLookahead(0, new byte[Constants.BLOCK_SIZE]));

        for (int i = (size + matrixHeight); i < (size + matrixHeight * 2); i++) {
            res.add(swapPartners.get(i - (size + matrixHeight)));
        }

//        Encrypt and write blocks to server
        List<BlockEncrypted> encryptedList = encryptBlocks(res);
        for (int i = 0; i < (size + matrixHeight * 2); i++) {
            if (!communicationStrategy.write(i, encryptedList.get(i)))
                return false;
        }

        return true;
    }

    boolean setup2(List<BlockStandard> blocks) {
        int blocksSize = blocks.size();
        if (blocksSize > size) {
            logger.error("Not possible to put: " + blocksSize + " blocks into a matrix of size: " + size);
            return false;
        }

        SecureRandom randomness = new SecureRandom();
        for (int i = blocksSize; i < size; i++) {
            blocks.add(new BlockStandard(0, new byte[Constants.BLOCK_SIZE]));
        }

        List<BlockLookahead> blockLookaheads = standardToLookaheadBlocksForSetup(blocks);
//        TODO: this does not seem to work
        Collections.shuffle(blockLookaheads, randomness);

        List<BlockEncrypted> encryptedBlocks = encryptBlocks(blockLookaheads);

        for (int i = 0; i < encryptedBlocks.size(); i++) {
            System.out.println("Writing block to communicationStrategy: #" + i);
            if (!communicationStrategy.write(i, encryptedBlocks.get(i)))
                return false;
        }

//        TODO: swap partners should be chosen before we write to the matrix
        for (int i = 0; i < matrixHeight; i++) {
            System.out.println("Choose initial swap partner: #" + i);
            Index index = null;
            boolean indexIsNotUnique = true;
            while (indexIsNotUnique) {
                index = new Index(randomness.nextInt(matrixHeight), randomness.nextInt(matrixHeight));
                Index finalIndex = index;
                indexIsNotUnique = futureSwapPartners.stream().anyMatch(s -> s.getIndex().equals(finalIndex));
            }
            BlockEncrypted blockRead = communicationStrategy.read(getFlatArrayIndex(index)); // TODO: handle the negative case

            futureSwapPartners.add(new SwapPartnerData(index, accessCounter));
            communicationStrategy.write(size + matrixHeight + accessCounter, blockRead); // TODO: handle the negative case
            accessCounter++;

            communicationStrategy.write(getFlatArrayIndex(index), getEncryptedDummy(secretKey, encryptionStrategy)); // TODO: handle the negative case
        }

//        Fill the access stash
        for (int i = 0; i < matrixHeight; i++) {
            communicationStrategy.write(size + i, getEncryptedDummy(secretKey, encryptionStrategy)); // TODO: handle the negative case
        }

        return true;
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

    @Override
    public byte[] access(OperationType op, int address, byte[] data) {
        Map<Integer, Map<Integer, BlockLookahead>> accessStash = getAccessStash();
        BlockLookahead[] swapStash = getSwapStash(); // TODO: sort by swap counter

        Index index = positionMap.get(address);
        BlockLookahead block = fetchBlockFromMatrix(index);

        boolean blockFoundInMatrix = true;
        boolean blockFoundInAccessStash = true;
        if (Util.isDummyAddress(block.getAddress())) {
            blockFoundInMatrix = false;
            block = findBlockInAccessStash(accessStash, index.getRowIndex(), index.getRowIndex());
            if (block == null) {
                blockFoundInAccessStash = false;
                block = findBlockInSwapStash(swapStash, index);
                if (block == null) {
                    logger.error("Unable to locate block");
                    return null;
                } else
                    logger.info("Block found in swap stash: " + block.toString());
            } else
                logger.info("Block found in access stash: " + block.toString());
        } else
            logger.info("Block found in matrix: " + block.toString());

        byte[] res = block.getData();

//        Get swap partner
        int swapIndex = Math.floorMod(accessCounter, matrixHeight);
        BlockLookahead swapPartner = swapStash[swapIndex];
        swapStash[swapIndex] = null;

//        Set index to index of swap partner
        block.setIndex(swapPartner.getIndex()); // TODO: does this work?
        positionMap.put(address, swapPartner.getIndex());

        if (op.equals(OperationType.WRITE))
            block.setData(data);

//        Update swap partner index and encrypt it
//        Index swapPartnerIndex = swapPartner.getIndex();
        swapPartner.setIndex(index);
        BlockEncrypted encryptedSwapPartner = encryptBlock(swapPartner);

        if (encryptedSwapPartner == null) return null;

        addToAccessStashMap(accessStash, block);

        int flatArrayIndex = getFlatArrayIndex(index);
        if (blockFoundInMatrix) {
            if (!communicationStrategy.write(flatArrayIndex, encryptedSwapPartner)) {
                logger.error("Unable to write swap partner to communicationStrategy: " + swapPartner.toString());
                return null;
            }
        } else if (blockFoundInAccessStash) {
            if (!communicationStrategy.write(flatArrayIndex, encryptedSwapPartner)) {
                logger.error("Unable to write swap partner to communicationStrategy: " + swapPartner.toString());
                return null;
            }
//            Remove old version of block
            accessStash = removeFromAccessStash(accessStash, index);
        } else {
            if (!communicationStrategy.write(flatArrayIndex, getEncryptedDummy(secretKey, encryptionStrategy))) {
                logger.error("Unable to write swap partner to communicationStrategy: dummy block");
                return null;
            }
            for (int i = 0; i < swapStash.length; i++) {
                BlockLookahead futureSwapPartner = swapStash[i];
                if (futureSwapPartner == null) continue;
                Index futureIndex = futureSwapPartner.getIndex();
                if (futureIndex.equals(index)) {
                    swapStash[i] = swapPartner;
                    break;
                }
            }
        }

        pickNewFutureSwapPartner();
        maintenanceJob(accessStash, swapStash);

        accessCounter++;
        return res;
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

    Map<Integer, Map<Integer, BlockLookahead>> addToAccessStashMap(Map<Integer, Map<Integer, BlockLookahead>> map,
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

    Map<Integer, Map<Integer, BlockLookahead>> removeFromAccessStash(Map<Integer, Map<Integer, BlockLookahead>> stash,
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
        int beginIndex = size + matrixHeight;
        int endIndex = size + matrixHeight * 2;
        int flattenedArrayOffSet = size + matrixHeight;

//        List<BlockLookahead> res = new ArrayList<>();
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

    BlockLookahead findBlockInAccessStash(Map<Integer, Map<Integer, BlockLookahead>> stash, int rowIndex,
                                          int colIndex) {
        if (stash.containsKey(colIndex)) {
            Map<Integer, BlockLookahead> columnMap = stash.get(colIndex);
            return columnMap.getOrDefault(rowIndex, null);
        }
        return null;
    }

    private BlockLookahead findBlockInSwapStash(BlockLookahead[] stash, Index index) {
        for (BlockLookahead block : stash) {
            if (block != null && block.getIndex().equals(index))
                return block;
        }
        return null;
    }

    private void pickNewFutureSwapPartner() {
        SecureRandom randomness = new SecureRandom();
        boolean futureSwapsContainsIndex = true;
        Index index = null;
        while (futureSwapsContainsIndex) {
            index = new Index(randomness.nextInt(matrixHeight), randomness.nextInt(matrixHeight));
            Index finalIndex = index;
            futureSwapsContainsIndex = futureSwapPartners.stream().anyMatch(f -> f.getIndex().equals(finalIndex));
        }
        futureSwapPartners.add(new SwapPartnerData(index, accessCounter));
    }

    BlockLookahead decryptToLookaheadBlock(BlockEncrypted blockEncrypted) {
        int endOfDataIndex = blockEncrypted.getData().length - Constants.BLOCK_SIZE * 2;
        byte[] encryptedData = Arrays.copyOfRange(blockEncrypted.getData(), 0, endOfDataIndex);
        byte[] encryptedIndex = Arrays.copyOfRange(blockEncrypted.getData(), endOfDataIndex, blockEncrypted.getData().length);
        byte[] data = encryptionStrategy.decrypt(encryptedData, secretKey);
        byte[] indices = encryptionStrategy.decrypt(encryptedIndex, secretKey);
        if (data == null) {
            logger.info("Tried to turn an encrypted block with value = null into a Lookahead block");
            return null;
        }


        int rowDataIndex = 0;
        int colDataIndex = INTEGER_BYTE_ARRAY_SIZE;
//        byte[] blockData = Arrays.copyOfRange(data, 0, rowDataIndex);
        byte[] rowIndexBytes = Arrays.copyOfRange(indices, rowDataIndex, colDataIndex);
        byte[] colIndexBytes = Arrays.copyOfRange(indices, colDataIndex, INTEGER_BYTE_ARRAY_SIZE * 2);

        BlockLookahead blockLookahead = new BlockLookahead();
        blockLookahead.setAddress(byteArrayToLeInt(encryptionStrategy.decrypt(blockEncrypted.getAddress(), secretKey)));
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
            byte[] encryptedAddress = encryptionStrategy.encrypt(Util.leIntToByteArray(block.getAddress()), secretKey);
            byte[] encryptedData = encryptionStrategy.encrypt(block.getData(), secretKey);
            byte[] encryptedIndex = encryptionStrategy.encrypt(ArrayUtils.addAll(rowIndexBytes, colIndexBytes),
                    secretKey);
            res.add(new BlockEncrypted(encryptedAddress, ArrayUtils.addAll(encryptedData, encryptedIndex)));
        }
        return res;
    }

    int getFlatArrayIndex(Index index) {
        int res = index.getRowIndex();
        res += index.getColIndex() * matrixHeight;
        return res;
    }

    //    TODO: make sure the stashes are filled with dummy blocks
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
                if (Util.isDummyAddress(swapPartner.getAddress())) {
                    logger.error("Trying to set a dummy block as swap partner with swap data: " + swap);
                    return false;
                }
                swapStash[Math.floorMod(swap.getSwapNumber(), matrixHeight)] = swapPartner;
                futureSwapPartners.remove(i);
                column.set(rowIndex, new BlockLookahead(0, new byte[Constants.BLOCK_SIZE]));
            }
        }

        List<BlockEncrypted> encryptedBlocks = encryptBlocks(column);
        if (encryptedBlocks.size() != matrixHeight) {
            logger.error("Wrong number of encrypted blocks to write back: " + encryptedBlocks.size());
        }
        for (int i = 0; i < matrixHeight; i++) {
            Index index = new Index(i, columnIndex);
            if (!communicationStrategy.write(getFlatArrayIndex(index), encryptedBlocks.get(i))) {
                logger.error("Unable to write block to communicationStrategy with index: " + index);
                return false;
            }
        }
        return true;
    }
}
