package oram.lookahead;


import oram.*;
import oram.server.Server;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private final int matrixHeight;
    private final byte[] key;
    private final Server server;
    private List<BlockLookahead> stash;
    private Map<Integer, Index> positionMap;
    private int accessCounter;

    public AccessStrategyLookahead(int size, int matrixWidth, byte[] key, Server server) {
        this.size = size;
        this.matrixHeight = matrixWidth;
        this.key = key;
        this.server = server;
        if (!(size == matrixWidth * matrixWidth))
            logger.error("Size of matrix is wrong");
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
        accessCounter++;

//        Set index to index of swap partner
        block.setIndex(swapPartner.getIndex());
        if (op.equals(OperationType.WRITE))
            block.setData(data);

//        Update swap partner index and encrypt it
//        Index swapPartnerIndex = swapPartner.getIndex();
        swapPartner.setIndex(index);
        BlockEncrypted encryptedBlock = encryptBlock(swapPartner);
        if (encryptedBlock == null)
            return null;

        addToAccessStashMap(accessStash, block);

        if (blockFoundInMatrix) {
            if (!server.write(getFlatArrayIndex(index), encryptedBlock)) {
                logger.error("Unable to write swap partner to server: " + swapPartner.toString());
            }
            accessStash.get(index.getColIndex()).remove(index.getRowIndex());
//            block.setIndex(swapPartnerIndex);
        } else if (blockFoundInAccessStash) {
            if (!server.write(getFlatArrayIndex(index), encryptedBlock)) {
                logger.error("Unable to write swap partner to server: " + swapPartner.toString());
            }
//            Remove old version of block

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
        
        return block.getData();
    }

    Map<Integer, Map<Integer, BlockLookahead>> getAccessStash() {
        int beginIndex = size;
        int endIndex = beginIndex + matrixHeight;

        Map<Integer, Map<Integer, BlockLookahead>> res = new HashMap<>();
        for (int i = beginIndex; i < endIndex; i++) {
            BlockLookahead blockLookahead = lookaheadBlockFromEncryptedBlock(server.read(i));
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

    List<BlockLookahead> getSwapStash() {
        int beginIndex = size + matrixHeight;
        int endIndex = size + matrixHeight * 2;

        List<BlockLookahead> res = new ArrayList<>();
        for (int i = beginIndex; i < endIndex; i++) {
            res.add(lookaheadBlockFromEncryptedBlock(server.read(i)));
        }
        return res;
    }

    BlockLookahead fetchBlockFromMatrix(Index index) {
        int serverIndex = getFlatArrayIndex(index);

        return lookaheadBlockFromEncryptedBlock(server.read(serverIndex));
    }

    BlockLookahead findBlockInAccessStash(Map<Integer, Map<Integer, BlockLookahead>> stash, int rowIndex, int colIndex) {
        if (stash.containsKey(colIndex)) {
            Map<Integer, BlockLookahead> columnMap = stash.get(colIndex);
            return columnMap.getOrDefault(rowIndex, null);
        }
        return null;
    }

    BlockLookahead findBlockInSwapStash(List<BlockLookahead> stash, Index index) {
        for (BlockLookahead block : stash) {
            if (block != null && block.getIndex().equals(index))
                return block;
        }
        return null;
    }

    BlockLookahead lookaheadBlockFromEncryptedBlock(BlockEncrypted blockEncrypted) {
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
}
