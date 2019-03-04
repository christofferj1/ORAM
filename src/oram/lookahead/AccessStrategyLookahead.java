package oram.lookahead;


import oram.AccessStrategy;
import oram.BlockEncrypted;
import oram.OperationType;
import oram.server.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 04-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class AccessStrategyLookahead implements AccessStrategy {
    private static final Logger logger = LogManager.getLogger("log");
    private final int size;
    private final int matrixSize;
    private final String key;
    private List<BlockLookahead> stash;
    private Map<Integer, Integer> positionMap;
    private final Server server;
    private int accessCounter;

    public AccessStrategyLookahead(int size, int matrixWidth, String key, Server server) {
        this.size = size;
        this.matrixSize = matrixWidth;
        this.key = key;
        this.server = server;
        if (!(size == matrixWidth * matrixWidth))
            logger.error("Size of matrix is wrong");
    }

    @Override
    public byte[] access(OperationType op, int address, byte[] data) {
        return null;
    }

    Map<Integer, Map<Integer, BlockLookahead>> getAccessStashStash() {
        int beginIndex = size;
        int endIndex = beginIndex + matrixSize;

        Map<Integer, Map<Integer, BlockLookahead>> res = new HashMap<>();
        for (int i = beginIndex; i < endIndex; i++) {
            BlockEncrypted block = server.read(i);
        }
        return null;
    }

    Map<Integer, Map<Integer, BlockLookahead>> addToAccessStashMap(Map<Integer, Map<Integer, BlockLookahead>> map,
                                                              int column, int row, BlockLookahead block) {
        if (map.containsKey(column)) {
            map.get(column).put(row, block);
        } else {
            map.put(column, new HashMap<>());
            map.get(column).put(row, block);
        }
        return map;
    }
}
