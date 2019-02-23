package oram.path;

import oram.*;
import oram.server.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.*;

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
            access(OperationType.WRITE, 1, new byte[Constants.BLOCK_SIZE]);
        }
        positionMap = new HashMap<>();
    }


    @Override
    public byte[] access(OperationType op, int address, byte[] data) {
        if (data != null && data.length > Constants.BLOCK_SIZE) {
            logger.error("Accessed with data length: " + data.length);
        }

        SecureRandom randomness = new SecureRandom();

//        Line 1 and 2 in pseudo code.
//        Return a random position if the block does not have one already
        int x = positionMap.getOrDefault(address, randomness.nextInt((int) (Math.pow(2, L - 1))));
        positionMap.put(address, randomness.nextInt((int) (Math.pow(2, L - 1))));

//        Line 3 to 5 in pseudo code.
        boolean readPath = readPathToStash(x);
        if (!readPath)
            return null;

//        Line 6 to 9 in pseudo code
        byte[] res = retrieveDataOverwriteBlock(address, op, data);

//        Line 10 to 15 in pseudo code.
        writeBackPath(address);

        return res;
    }

    private boolean readPathToStash(int address) {
        boolean res = true;
        for (int l = 0; l < L; l++) {
            int position = getPosition(address, l);
            List<BlockPath> bucket = new ArrayList<>();
            for (int i = 0; i < bucketSize; i++) {
                bucket.add((BlockPath) server.read(position + i));
            }

            if (bucket.size() == bucketSize) {
                for (BlockPath block : bucket) {
                    byte[] message = AES.decrypt(block.getData(), key);

                    if (message == null || Util.isDummyBlock(message))
                        continue;

                    int blockAddress = block.getAddress();
                    stash.add(new BlockPath(blockAddress, message));
                }
            } else {
                logger.error("Reading bucket for position: " + address + ", in layer: " + l + " failed");
                res = false;
                break;
            }
        }
        return res;
    }

    /**
     * As their might not be a block to overwrite, we keep track of whether or not any block has been overwritten
     *
     * @return the data the access must return at last
     */
    private byte[] retrieveDataOverwriteBlock(int address, OperationType op, byte[] data) {
        boolean hasOverwrittenBlock = false;
        byte[] endData = null;
        for (int i = 0; i < stash.size(); i++) {
            if (stash.get(i).getAddress() == address) {
                endData = stash.get(i).getData();
                if (op.equals(OperationType.WRITE)) {
                    stash.set(i, new BlockPath(address, data));
                    hasOverwrittenBlock = true;
                }
                break;
            }
        }

        if (op.equals(OperationType.WRITE) && !hasOverwrittenBlock)
            stash.add(new BlockPath(address, data));

        return endData;
    }

    private void writeBackPath(int address) {
        for (int l = L - 1; l >= 0; l--) {
            List<BlockPath> blocksToWrite = new ArrayList<>();
            int position = getPosition(address, l);
            List<Integer> indices = getSubTreeNodes(position);

//            Pick all the blocks from the stash which can be written to the current node
            for (BlockPath s : stash) {
                if (indices.contains(positionMap.get(s.getAddress())))
                    blocksToWrite.add(s);
            }

//            Make sure there are exactly Z blocks to write to the node
            if (blocksToWrite.size() > bucketSize) {
                for (int i = blocksToWrite.size(); i >= bucketSize; i--)
                    blocksToWrite.remove(i);
            } else if (blocksToWrite.size() < bucketSize) {
                blocksToWrite = fillWithDummy(blocksToWrite);
            }

//            Remove the blocks from the stash, before they are written to the node
            for (int i = stash.size() - 1; i >= 0; i--) {
                if (blocksToWrite.contains(stash.get(i)))
                    stash.remove(i);
            }

//            Encrypts all pairs
            for (int i = 0; i < blocksToWrite.size(); i++) {
                BlockPath block = blocksToWrite.get(i);
                byte[] addressSized = Util.sizedByteArrayWithInt(block.getAddress(), L);
                byte[] dataTmp = new byte[Constants.BLOCK_SIZE];
                System.arraycopy(block.getData(), 0, dataTmp, 0, dataTmp.length);

                server.write(position + i, new BlockEncrypted(addressSized, dataTmp));
            }
        }
    }

    private List<BlockPath> fillWithDummy(List<BlockPath> temp) {
        for (int i = temp.size(); i < bucketSize; i++) {
            temp.add(new BlockPath(Constants.DUMMY_BLOCK_ADDRESS, new byte[Constants.BLOCK_SIZE]));
        }
        return temp;
    }

    private List<Integer> getSubTreeNodes(int position) {
        if (position > Math.pow(2, L - 1) - 2)
            return Collections.singletonList(position - ((int) (Math.pow(2, L - 1) - 1)));

        List<Integer> res = new ArrayList<>();
        res.addAll(getSubTreeNodes(2 * position + 1));
        res.addAll(getSubTreeNodes(2 * position + 2));

        return res;
    }

    /**
     * Calculates the position in the flattened tree based on position and level. When the bucket size is bigger than 1,
     * the position of the first block is returned.
     *
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
