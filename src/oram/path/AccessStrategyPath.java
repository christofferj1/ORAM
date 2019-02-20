package oram.path;

import oram.*;
import oram.server.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 19-02-2019. <br>
 * Copyright (C) Lind Invest 2019 </p>
 */

public class AccessStrategyPath implements AccessStrategy {
    private static final Logger logger = LogManager.getLogger("log");
    private final int L;
    private final int bucketSize;
    private final String key;
    private List<BlockPath> stash;
    private Map<Integer, Integer> positionMap;
    private Server server;

    public AccessStrategyPath(int size, Server server, int bucketSize, String key) {
        this.stash = new ArrayList<>();
        this.positionMap = new HashMap<>();
        this.bucketSize = bucketSize;
        this.L = (int) Math.ceil(Math.log(size) / Math.log(2));
        this.server = server;
        this.key = key;

        initializeServer();
    }

    private void initializeServer() {
        double numberOfLeaves = Math.pow(2, L - 1);
        for (int i = 0; i < numberOfLeaves; i++) {
            positionMap.put(1, i);
            access(OperationType.WRITE, 1, new Byte[Constants.BLOCK_SIZE]);
        }
        positionMap = new HashMap<>();
    }


    @Override
    public byte[] access(OperationType op, int address, Byte[] data) {
        if (data != null && data.length > Constants.BLOCK_SIZE) {
            logger.error("Accessed with data length: " + data.length);
        }

        SecureRandom randomness = new SecureRandom();

//        Line 1 and 2 in pseudo code.
//        Return a random position if the block does not have one already
        int x = positionMap.getOrDefault(address, randomness.nextInt((int) (Math.pow(2, L - 1))));
        positionMap.put(address, randomness.nextInt((int) (Math.pow(2, L - 1))));

//        Line 3 to 5 in pseudo code.
        for (int l = 0; l < L; l++) {
            int position = getPosition(x, l);
            List<BlockPath> res = new ArrayList<>();
            for (int i = 0; i < bucketSize; i++) {
                res.add((BlockPath) server.read(position + i));
            }

            if (res.size() == bucketSize) {
                for (BlockPath block : res) {
                    byte[] message = AES.decrypt(block.getData(), key);

                    if (message == null || Util.isDummyBlock(message))
                        continue;

                    int blockAddress = block.getAddress();
                    stash.add(new BlockPath(blockAddress, message));
                }
            }
        }

        return null;
    }

    /**
     * Calculates the position in the flattened tree based on position and level. When the bucket size is bigger than 1,
     * the position of the first block is returned.
     * @return position of first block in bucket in the flattened tree
     */
    int getPosition(int position, int level) {
        int res = (int) (Math.pow(2, L - 1) - 1 + position);
        for (int i = L - 1; i > level; i--) {
            res = (int) Math.floor((res - 1) / 2);
        }
        return res * bucketSize;
    }

}
