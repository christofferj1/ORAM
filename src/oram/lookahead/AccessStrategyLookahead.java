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
    private Map<Integer, Integer> positionMap;
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
        return null;
    }

    //    TODO: Test this
    Map<Integer, Map<Integer, BlockLookahead>> getAccessStash() {
        int beginIndex = size;
        int endIndex = beginIndex + matrixHeight;

        Map<Integer, Map<Integer, BlockLookahead>> res = new HashMap<>();
        for (int i = beginIndex; i < endIndex; i++) {
            BlockLookahead blockLookahead = lookaheadBlockFromEncryptedBlock(server.read(i));
            res = addToAccessStashMap(res, blockLookahead);
        }

        return null;
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

    //    TODO: test this
    List<BlockLookahead> getSwapStash() {
        int beginIndex = size + matrixHeight;
        int endIndex = size + matrixHeight * 2;

        List<BlockLookahead> res = new ArrayList<>();
        for (int i = beginIndex; i < endIndex; i++) {
            res.add(lookaheadBlockFromEncryptedBlock(server.read(i)));
        }
        return res;
    }

    BlockLookahead lookaheadBlockFromEncryptedBlock(BlockEncrypted blockEncrypted) {
        byte[] data = AES.decrypt(blockEncrypted.getData(), key);
        if (data == null) return null;
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
}
