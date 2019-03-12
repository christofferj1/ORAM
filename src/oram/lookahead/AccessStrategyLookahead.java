package oram.lookahead;


import oram.*;
import oram.clientcom.Server;
import oram.path.BlockStandard;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

public class AccessStrategyLookahead implements AccessStrategy {
    private static final Logger logger = LogManager.getLogger("log");
    private final int size;
    private final int matrixHeight; // Assumes to be equal to matrix width
    private final byte[] key;
    private final Server server;
    private Map<Integer, Index> positionMap;
    private int accessCounter;
    private List<SwapPartnerData> futureSwapPartners;

    public AccessStrategyLookahead(int size, int matrixHeight, byte[] key, Server server) {
        this.size = size;
        this.matrixHeight = matrixHeight;
        this.key = key;
        this.server = server;
        if (!(size == matrixHeight * matrixHeight))
            logger.error("Size of matrix is wrong");
        accessCounter = 0;
        futureSwapPartners = new ArrayList<>();
        positionMap = new HashMap<>();
    }

    boolean setup(List<BlockStandard> blocks) {
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
        Collections.shuffle(blocks, randomness);

        List<BlockEncrypted> encryptedBlocks = encryptBlocks(blockLookaheads);

        for (int i = 0; i < encryptedBlocks.size(); i++) {
            System.out.println("Writing block to server: #" + i);
            if (!server.write(i, encryptedBlocks.get(i)))
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
            BlockEncrypted blockRead = server.read(getFlatArrayIndex(index)); // TODO: handle the negative case

            futureSwapPartners.add(new SwapPartnerData(index, accessCounter));
            server.write(size + matrixHeight + accessCounter, blockRead); // TODO: handle the negative case
            accessCounter++;

//            TODO: Create dummy block strategy
            BlockEncrypted dummyBlock = new BlockEncrypted(
                    AES.encrypt(Util.leIntToByteArray(0), key),
                    AES.encrypt(new byte[Constants.BLOCK_SIZE], key));
            server.write(getFlatArrayIndex(index), dummyBlock); // TODO: handle the negative case
        }

        for (int i = 0; i < matrixHeight; i++) {
            BlockEncrypted dummyBlock = new BlockEncrypted(
                    AES.encrypt(Util.leIntToByteArray(0), key),
                    AES.encrypt(new byte[Constants.BLOCK_SIZE], key));
            server.write(size + i, dummyBlock); // TODO: handle the negative case
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
                positionMap.put(blockStandard.getAddress(), index);
            }
        }
        return res;
    }

    @Override
    public byte[] access(OperationType op, int address, byte[] data) {
        Map<Integer, Map<Integer, BlockLookahead>> accessStash = getAccessStash();
        List<BlockLookahead> swapStash = getSwapStash();

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

//        Get swap partner
        BlockLookahead swapPartner = swapStash.remove(Math.floorMod(accessCounter, matrixHeight));

//        Set index to index of swap partner
        block.setIndex(swapPartner.getIndex());
        positionMap.put(address, swapPartner.getIndex());

        if (op.equals(OperationType.WRITE)) {block.setData(data);}

//        Update swap partner index and encrypt it
//        Index swapPartnerIndex = swapPartner.getIndex();
        swapPartner.setIndex(index);
        BlockEncrypted encryptedSwapPartner = encryptBlock(swapPartner);

        if (encryptedSwapPartner == null) {return null;}

        addToAccessStashMap(accessStash, block);

        if (blockFoundInMatrix) {
            if (!server.write(getFlatArrayIndex(index), encryptedSwapPartner)) {
                logger.error("Unable to write swap partner to server: " + swapPartner.toString());
            }
        } else if (blockFoundInAccessStash) {
            if (!server.write(getFlatArrayIndex(index), encryptedSwapPartner)) {
                logger.error("Unable to write swap partner to server: " + swapPartner.toString());
            }
//            Remove old version of block
//            accessStash.get(index.getColIndex()).remove(index.getRowIndex());
            accessStash = removeFromAccessStash(accessStash, index);
        } else {
            BlockEncrypted dummyBlock = new BlockEncrypted(
                    AES.encrypt(Util.leIntToByteArray(0), key),
                    AES.encrypt(new byte[Constants.BLOCK_SIZE], key));
            if (!server.write(getFlatArrayIndex(index), dummyBlock)) {
                logger.error("Unable to write swap partner to server: dummy block");
            }
            for (int i = 0; i < matrixHeight; i++) {
                if (swapStash.get(i).getIndex().equals(index)) {
                    swapStash.set(i, swapPartner);
                    break;
                }
            }
        }

        pickNewFutureSwapPartner();
        maintenanceJob(accessStash, swapStash);

        accessCounter++;
        return block.getData();
    }

    Map<Integer, Map<Integer, BlockLookahead>> getAccessStash() {
        int beginIndex = size;
        int endIndex = beginIndex + matrixHeight;

        Map<Integer, Map<Integer, BlockLookahead>> res = new HashMap<>();
        for (int i = beginIndex; i < endIndex; i++) {
            BlockLookahead blockLookahead = decryptToLookaheadBlock(server.read(i));
            res = addToAccessStashMap(res, blockLookahead);
        }

        return res;
    }

    Map<Integer, Map<Integer, BlockLookahead>> addToAccessStashMap(Map<Integer, Map<Integer, BlockLookahead>> map,
                                                                   BlockLookahead block) {
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

    List<BlockLookahead> getSwapStash() {
        int beginIndex = size + matrixHeight;
        int endIndex = size + matrixHeight * 2;

        List<BlockLookahead> res = new ArrayList<>();
        for (int i = beginIndex; i < endIndex; i++) {
            res.add(decryptToLookaheadBlock(server.read(i)));
        }
        return res;
    }

    BlockLookahead fetchBlockFromMatrix(Index index) {
        int serverIndex = getFlatArrayIndex(index);

        return decryptToLookaheadBlock(server.read(serverIndex));
    }

    BlockLookahead findBlockInAccessStash(Map<Integer, Map<Integer, BlockLookahead>> stash, int rowIndex, int colIndex) {
        if (stash.containsKey(colIndex)) {
            Map<Integer, BlockLookahead> columnMap = stash.get(colIndex);
            return columnMap.getOrDefault(rowIndex, null);
        }
        return null;
    }

    private BlockLookahead findBlockInSwapStash(List<BlockLookahead> stash, Index index) {
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
        futureSwapPartners.set(Math.floorMod(accessCounter, matrixHeight), new SwapPartnerData(index, accessCounter));
    }

    BlockLookahead decryptToLookaheadBlock(BlockEncrypted blockEncrypted) {
        byte[] data = AES.decrypt(blockEncrypted.getData(), key);
        if (data == null) {
            logger.info("Tried to turn an encrypted block with value = null into a Lookahead block");
            return null;
        }
        int rowDataIndex = data.length - (INTEGER_BYTE_ARRAY_SIZE * 2);
        int colDataIndex = data.length - INTEGER_BYTE_ARRAY_SIZE;
        byte[] blockData = Arrays.copyOfRange(data, 0, rowDataIndex);
        byte[] rowIndexBytes = Arrays.copyOfRange(data, rowDataIndex, colDataIndex);
        byte[] colIndexBytes = Arrays.copyOfRange(data, colDataIndex, data.length);

        BlockLookahead blockLookahead = new BlockLookahead();
        blockLookahead.setAddress(byteArrayToLeInt(AES.decrypt(blockEncrypted.getAddress(), key)));
        blockLookahead.setData(blockData);
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
            res.add(new BlockEncrypted(
                    AES.encrypt(Util.leIntToByteArray(block.getAddress()), key),
                    AES.encrypt(ArrayUtils.addAll(
                            ArrayUtils.addAll(block.getData(), rowIndexBytes), colIndexBytes), key)));
        }
        return res;
    }

    int getFlatArrayIndex(Index index) {
        int res = index.getRowIndex();
        res += index.getColIndex() * matrixHeight;
        return res;
    }

    boolean maintenanceJob(Map<Integer, Map<Integer, BlockLookahead>> accessStash, List<BlockLookahead> swapStash) {
        int columnIndex = Math.floorMod(accessCounter, matrixHeight);
        List<BlockLookahead> column = new ArrayList<>();

//        Retrieve column from matrix
        for (int i = 0; i < matrixHeight; i++) {
            BlockEncrypted encryptedBlock = server.read(getFlatArrayIndex(new Index(i, columnIndex)));
            if (encryptedBlock == null) {
                logger.error("Unable to read block with index (" + i + ", " + columnIndex + ") from server");
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
        for (SwapPartnerData swap : futureSwapPartners) {
            if (swap.getIndex().getColIndex() == columnIndex) {
                int rowIndex = swap.getIndex().getRowIndex();
                BlockLookahead swapPartner = column.get(rowIndex);
                if (Util.isDummyAddress(swapPartner.getAddress())) {
                    logger.error("Trying to set a dummy block as swap partner with swap data: " + swap);
                    return false;
                }
                swapStash.set(Math.floorMod(swap.getSwapNumber(), matrixHeight), swapPartner);
                column.set(rowIndex, new BlockLookahead(0, new byte[Constants.BLOCK_SIZE]));
            }
        }

        List<BlockEncrypted> encryptedBlocks = encryptBlocks(column);
        if (encryptedBlocks.size() != matrixHeight) {
            logger.error("Wrong number of encrypted blocks to write back: " + encryptedBlocks.size());
        }
        for (int i = 0; i < matrixHeight; i++) {
            Index index = new Index(i, columnIndex);
            if (!server.write(getFlatArrayIndex(index), encryptedBlocks.get(i))) {
                logger.error("Unable to write block to server with index: " + index);
                return false;
            }
        }
        return true;
    }
}
