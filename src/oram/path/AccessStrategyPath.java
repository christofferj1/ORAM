package oram.path;

import oram.*;
import oram.server.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.*;

import static oram.Constants.ADDRESS_SIZE;

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
    private boolean print;
    private int dummyCounter = 0;

    AccessStrategyPath(int size, Server server, int bucketSize, String key) {
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
            System.out.println("Round: " + i + "\n" + server.getTreeString());
            positionMap.put(0, i);
            access(OperationType.WRITE, 0, new byte[Constants.BLOCK_SIZE]);
        }
        System.out.println("Initialized\n" + server.getTreeString());
        positionMap = new HashMap<>();
        print = true;
    }


    @Override
    public byte[] access(OperationType op, int address, byte[] data) {
        if (data != null && data.length > Constants.BLOCK_SIZE) {
            logger.error("Accessed with data length: " + data.length);
        }

        SecureRandom randomness = new SecureRandom();

//        Line 1 and 2 in pseudo code.
//        Return a random position if the block does not have one already
        int leafNodeIndex = positionMap.getOrDefault(address, randomness.nextInt((int) (Math.pow(2, L - 1))));
        if (print) {
            System.out.println("MAP BEFORE");
            for (Map.Entry<Integer, Integer> entry : positionMap.entrySet()) {
                System.out.println(entry.getKey() + " -> " + entry.getValue());
            }
        }
        positionMap.put(address, randomness.nextInt((int) (Math.pow(2, L - 1))));

        if (print) {
            System.out.println("Address: " + address + ", leaf node index: " + leafNodeIndex + ", data: " + (data == null ? null : new String(data)));
            System.out.println("MAP AFTER");
            for (Map.Entry<Integer, Integer> entry : positionMap.entrySet()) {
                System.out.println(entry.getKey() + " -> " + entry.getValue());
            }
        }

//        Line 3 to 5 in pseudo code.
        boolean readPath = readPathToStash(leafNodeIndex);
        if (!readPath)
            return null;

//        Line 6 to 9 in pseudo code
        byte[] res = retrieveDataOverwriteBlock(address, op, data);

//        Line 10 to 15 in pseudo code.
        writeBackPath(leafNodeIndex);

        return res;
    }

    private boolean readPathToStash(int leafNodeIndex) {
        boolean res = true;
        for (int l = 0; l < L; l++) {
            int position = getPosition(leafNodeIndex, l);
            List<BlockEncrypted> bucket = new ArrayList<>();
            for (int i = 0; i < bucketSize; i++) {
                bucket.add(server.read(position + i));
            }

            if (bucket.size() == bucketSize) {
                for (BlockEncrypted block : bucket) {
                    if (block == null || block.getAddress() == null || block.getData() == null) continue;

//                    TODO: ENCRYPT AGAIN
//                    byte[] message = AES.decrypt(block.getData(), key);
//                    byte[] addressBytes = AES.decrypt(block.getAddress(), key);
//                    if (addressBytes == null) continue;

//                    int blockAddress = ByteBuffer.wrap(addressBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

//                    TODO: check if isDummyAddress working
//                    if (message == null || Util.isDummyAddress(blockAddress)) continue;

//                    stash.add(new BlockPath(blockAddress, message));
                    int addressInt = ByteBuffer.wrap(block.getAddress()).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    if (Util.isDummyAddress(addressInt)) continue;
                    stash.add(new BlockPath(addressInt, block.getData()));
                }
            } else {
                logger.error("Reading bucket for position: " + leafNodeIndex + ", in layer: " + l + " failed");
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

    private void writeBackPath(int leafNode) {
        for (int l = L - 1; l >= 0; l--) {
            List<BlockPath> blocksToWrite = new ArrayList<>();
            int nodeNumber = getNode(leafNode, l);
            int arrayPosition = nodeNumber * bucketSize;
            List<Integer> indices = getSubTreeNodes(nodeNumber);

//            Pick all the blocks from the stash which can be written to the current node
            for (BlockPath s : stash) {
                if (indices.contains(positionMap.get(s.getAddress())))
                    blocksToWrite.add(s);
            }

//            Make sure there are exactly Z blocks to write to the node
            if (blocksToWrite.size() > bucketSize) {
                System.out.println("Bucket size: " + bucketSize);
                System.out.println("Blocks to right size: " + blocksToWrite.size());
                for (int i = blocksToWrite.size(); i > bucketSize; i--)
                    blocksToWrite.remove(i - 1);
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
                byte[] addressSized = Util.sizedByteArrayWithInt(block.getAddress(), ADDRESS_SIZE);
                byte[] addressCipher = AES.encrypt(addressSized, key);

                byte[] dataTmp = new byte[Constants.BLOCK_SIZE];
                byte[] data = block.getData();
                System.arraycopy(data, 0, dataTmp, 0, data.length);
                byte[] dataCipher = AES.encrypt(dataTmp, key);

//                TODO: make sure it is switched back to the encrypted version again
//                TODO: permute the blocks before writing them back
                server.write(arrayPosition + i, new BlockEncrypted(addressSized, dataTmp));
            }
        }
    }

    private List<BlockPath> fillWithDummy(List<BlockPath> temp) {
        for (int i = temp.size(); i < bucketSize; i++) {
//            TODO: the dummy data should be zero's
            temp.add(new BlockPath(Constants.DUMMY_BLOCK_ADDRESS, Util.sizedByteArrayWithInt(dummyCounter++, Constants.BLOCK_SIZE)));
        }
        return temp;
    }

    //    Package private for tests
    List<Integer> getSubTreeNodes(int position) {
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
    int getPosition(int leafNode, int level) {
        int res = (int) (Math.pow(2, L - 1) - 1 + leafNode);
        for (int i = L - 1; i > level; i--) {
            res = (int) Math.floor((res - 1) / 2);
        }
        return res * bucketSize;
    }

    /**
     * Calculates the position in the tree based on position and level.
     *
     * @return position of first block in bucket in the flattened tree
     */
    int getNode(int leafNode, int level) {
        int res = (int) (Math.pow(2, L - 1) - 1 + leafNode);
        for (int i = L - 1; i > level; i--) {
            res = (int) Math.floor((res - 1) / 2);
        }
        return res;
    }
}
