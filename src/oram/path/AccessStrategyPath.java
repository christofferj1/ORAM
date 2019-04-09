package oram.path;

import oram.AccessStrategy;
import oram.Constants;
import oram.OperationType;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockPath;
import oram.block.BlockStandard;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.factory.Factory;
import oram.permutation.PermutationStrategy;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.*;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 19-02-2019. <br>
 */

public class AccessStrategyPath implements AccessStrategy {
    private static final Logger logger = LogManager.getLogger("log");
    private final int L;
    private final int bucketSize;
    private final int leafCount;
    private final SecretKey secretKey;
    private final CommunicationStrategy communicationStrategy;
    private final EncryptionStrategy encryptionStrategy;
    private final PermutationStrategy permutationStrategy;
    private int maxStashSize;
    private int maxStashSizeBetweenAccesses;
    private List<BlockPath> stash;
    private Map<Integer, Integer> positionMap;
    private boolean print = true;
    private int dummyCounter = 0;

    public AccessStrategyPath(int size, int bucketSize, byte[] key, Factory factory) {
        this.bucketSize = bucketSize;
        stash = new ArrayList<>();
        positionMap = new HashMap<>();
        L = (int) Math.ceil(Math.log(size) / Math.log(2));
        leafCount = (int) (Math.pow(2, L - 1));
        communicationStrategy = factory.getCommunicationStrategy();
        encryptionStrategy = factory.getEncryptionStrategy();
        secretKey = encryptionStrategy.generateSecretKey(key);
        permutationStrategy = factory.getPermutationStrategy();
        maxStashSize = 0;
        maxStashSizeBetweenAccesses = 0;

        logger.info("######### Initialized Path ORAM strategy #########");
        logger.debug("######### Initialized Path ORAM strategy #########");
    }

    public void setup() {
        double numberOfLeaves = Math.pow(2, L - 1);
        for (int i = 0; i < numberOfLeaves; i++) {
            positionMap.put(0, i);
            access(OperationType.WRITE, 0, new byte[Constants.BLOCK_SIZE]);
        }
        positionMap = new HashMap<>();
    }

    @Override
    public boolean setup(List<BlockStandard> blocks) {
        return true;
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
            System.out.println("Access op: " + op.toString() + ", address: " + address + ", data: " + Util.getShortDataString(data));
            System.out.println("MAP BEFORE");
            for (Map.Entry<Integer, Integer> entry : positionMap.entrySet())
                System.out.print(StringUtils.leftPad(String.valueOf(entry.getKey()), 2) + " -> " +
                        StringUtils.leftPad(String.valueOf(entry.getValue()), 2) + ", ");
            System.out.println(" ");
        }
        int newLeafNodeIndex = randomness.nextInt((int) (Math.pow(2, L - 1)));
        positionMap.put(address, newLeafNodeIndex);

        if (print) {
            System.out.println("MAP AFTER");
            for (Map.Entry<Integer, Integer> entry : positionMap.entrySet())
                System.out.print(StringUtils.leftPad(String.valueOf(entry.getKey()), 2) + " -> " +
                        StringUtils.leftPad(String.valueOf(entry.getValue()), 2) + ", ");
            System.out.println(" ");
            System.out.println("Leaf node changed from: " + leafNodeIndex + " to: " + positionMap.get(address));
        }

        logger.info("Access op: " + op.toString() + ", address: " + address + ", leaf node: "
                + leafNodeIndex + " -> " + positionMap.get(address));

//        Line 3 to 5 in pseudo code.
        boolean readPath = readPathToStash(leafNodeIndex);
        if (!readPath) {
            logger.error("Unable to read path doing access");
            return null;
        }

//        Line 6 to 9 in pseudo code
        byte[] res = retrieveDataOverwriteBlock(address, op, data, newLeafNodeIndex);
        if (res == null) {
            logger.error("Unable to retrieve data from address: " + address);
            res = new byte[0];
        }

//        Line 10 to 15 in pseudo code.
        boolean writeBack = writeBackPath(leafNodeIndex);
        if (!writeBack) {
            logger.error("Unable to write back path with doing access");
            return null;
        }

        if (stash.size() > maxStashSizeBetweenAccesses) {
            maxStashSizeBetweenAccesses = stash.size();
            logger.info("Max stash size between accesses: " + maxStashSizeBetweenAccesses);
        }

        if (print) System.out.println("Returning data: " + Util.getShortDataString(res));

        return res;
    }

    private boolean readPathToStash(int leafNodeIndex) {
        if (print) System.out.println("Read path");
        boolean res = true;
        List<Integer> positionsToRead = new ArrayList<>();
        for (int l = 0; l < L; l++) {
            int nodeNumber = getNode(leafNodeIndex, l);
            int position = nodeNumber * bucketSize;
            if (print) System.out.println("    Read node: " + nodeNumber);

            for (int i = 0; i < bucketSize; i++)
                positionsToRead.add(position + i);

        }

        List<BlockEncrypted> encryptedBlocks = communicationStrategy.readArray(positionsToRead);

        if (encryptedBlocks == null || bucketSize * L != encryptedBlocks.size()) {
            logger.error("Did not fetch the right amount of blocks");
            res = false;
        } else {
            List<BlockPath> blocksDecrypted = decryptBlockPaths(encryptedBlocks, true);
            if (blocksDecrypted == null) {
                logger.error("Unable to decrypt path of blocks");
                res = false;
            } else {

                if (print) {
                    System.out.println("    Found blocks: ");
                    for (BlockPath b : blocksDecrypted) System.out.println("        " + b.toStringShort());
                }

                stash.addAll(blocksDecrypted);
                if (stash.size() > maxStashSize) {
                    maxStashSize = stash.size();
                    logger.info("Max stash size: " + maxStashSize);
                }
            }
        }
        return res;
    }

    /**
     * As their might not be a block to overwrite, we keep track of whether or not any block has been overwritten
     *
     * @return the data the access must return at last
     */
    private byte[] retrieveDataOverwriteBlock(int address, OperationType op, byte[] data, int newLeadNodeIndex) {
        if (print) System.out.println("Retrieve data and overwrite block");
        boolean hasOverwrittenBlock = false;
        byte[] endData = null;
        for (int i = 0; i < stash.size(); i++) {
            if (stash.get(i).getAddress() == address) {
                endData = stash.get(i).getData();
                if (print) System.out.println("    Retrieving end data: " + Util.getShortDataString(endData));
                if (op.equals(OperationType.WRITE)) {
                    if (print) System.out.println("    Overwrites with new data");
                    stash.set(i, new BlockPath(address, data, newLeadNodeIndex));
                    hasOverwrittenBlock = true;
                }
                break;
            }
        }

        if (op.equals(OperationType.WRITE) && !hasOverwrittenBlock) {
            stash.add(new BlockPath(address, data, newLeadNodeIndex));
            if (print) System.out.println("    Adding new block to stash");
        }
        if (stash.size() > maxStashSize) {
            maxStashSize = stash.size();
            logger.info("Max stash size: " + maxStashSize);
        }

        return endData;
    }

    private boolean writeBackPath(int leafNode) {
        if (print) {
            System.out.println("Write back path");
            System.out.println("    Stash:");
            System.out.print("        ");
            for (BlockPath b : stash) System.out.print(b.toStringShort() + ", ");
            System.out.println(" ");
        }

        List<Integer> addressesToWrite = new ArrayList<>();
        List<BlockEncrypted> encryptedBlocksToWrite = new ArrayList<>();
        for (int l = L - 1; l >= 0; l--) {
            int nodeNumber = getNode(leafNode, l);
            int arrayPosition = nodeNumber * bucketSize;
            if (print) System.out.println("    Node number: " + nodeNumber + ", array position: " + arrayPosition);

//            Pick all the blocks from the stash which can be written to the current node
            List<BlockPath> blocksToWrite = getBlocksForNode(nodeNumber);
            if (print) {
                System.out.println("    Blocks to write to node number: " + nodeNumber);
                System.out.print("        ");
                for (BlockPath b : blocksToWrite) System.out.print(b.toStringShort() + ", ");
                System.out.println(" ");
            }

//            Make sure there are exactly Z blocks to write to the node
            blocksToWrite = fillBucketWithDummyBlocks(blocksToWrite);
            blocksToWrite = permutationStrategy.permutePathBlocks(blocksToWrite);
            if (print) {
                System.out.println("    Blocks actually written:");
                System.out.print("        ");
                for (BlockPath b : blocksToWrite) System.out.print(b.toStringShort() + ", ");
                System.out.println(" ");
            }

//            Remove the blocks from the stash, before they are written to the node
            removeBlocksFromStash(blocksToWrite);
            if (print) {
                System.out.println("    Stash:");
                System.out.print("        ");
                for (BlockPath b : stash) System.out.print(b.toStringShort() + ", ");
                System.out.println(" ");
            }

//            Encrypts all pairs
            List<BlockEncrypted> encryptedBlocksToWriteTmp = encryptBucketOfBlocks(blocksToWrite);
            if (encryptedBlocksToWriteTmp == null) {
                logger.error("Returned null when trying to encrypt blocks");
                return false;
            }
            for (int i = 0; i < blocksToWrite.size(); i++) {
                addressesToWrite.add(arrayPosition + i);
                encryptedBlocksToWrite.add(encryptedBlocksToWriteTmp.get(i));
            }
        }

        if (!communicationStrategy.writeArray(addressesToWrite, encryptedBlocksToWrite)) {
            logger.error("Writing returned unsuccessful");
            return false;
        }
        return true;
    }

    private List<BlockPath> getBlocksForNode(int nodeIndex) {
        List<Integer> indices = getSubTreeNodes(nodeIndex);
        List<BlockPath> res = new ArrayList<>();
        for (BlockPath s : stash) {
            for (Integer i : indices) {
                if (i.equals(s.getIndex())) {
//            if (indices.contains(positionMap.get(s.getAddress())))
                    res.add(s);
                    break;
                }
            }
        }
        return res;
    }

    private List<BlockPath> fillBucketWithDummyBlocks(List<BlockPath> blocksToWrite) {
        List<BlockPath> res = new ArrayList<>(blocksToWrite);
        if (blocksToWrite.size() > bucketSize) {
            res = removeOverflowingBlocks(res);
        } else if (blocksToWrite.size() < bucketSize) {
            res = fillWithDummy(res);
        }
        return res;
    }

    private List<BlockPath> removeOverflowingBlocks(List<BlockPath> blocksToWrite) {
        List<BlockPath> res = new ArrayList<>(blocksToWrite);
        for (int i = blocksToWrite.size(); i > bucketSize; i--)
            res.remove(i - 1);
        return res;
    }

    private void removeBlocksFromStash(List<BlockPath> blocksToWrite) {
        for (int i = stash.size() - 1; i >= 0; i--) {
            if (blocksToWrite.contains(stash.get(i)))
                stash.remove(i);
        }
    }

    private List<BlockEncrypted> encryptBucketOfBlocks(List<BlockPath> blocksToWrite) {
        List<BlockEncrypted> encryptedBlocksToWrite = new ArrayList<>();
        for (BlockPath block : blocksToWrite) {
            byte[] addressCipher = encryptionStrategy.encrypt(Util.leIntToByteArray(block.getAddress()), secretKey);
            byte[] indexCipher = encryptionStrategy.encrypt(Util.leIntToByteArray(block.getIndex()), secretKey);
            byte[] dataCipher = encryptionStrategy.encrypt(block.getData(), secretKey);
            if (addressCipher == null || indexCipher == null || dataCipher == null) {
                logger.error("Unable to encrypt address: " + block.getAddress() + " or data");
                return null;
            }
            byte[] encryptedDataPlus = ArrayUtils.addAll(dataCipher, indexCipher);
            encryptedBlocksToWrite.add(new BlockEncrypted(addressCipher, encryptedDataPlus));
        }
        encryptedBlocksToWrite = permutationStrategy.permuteEncryptedBlocks(encryptedBlocksToWrite);
        return encryptedBlocksToWrite;
    }


    private List<BlockPath> fillWithDummy(List<BlockPath> temp) {
        for (int i = temp.size(); i < bucketSize; i++) {
            temp.add(new BlockPath(Constants.DUMMY_BLOCK_ADDRESS, new byte[Constants.BLOCK_SIZE], 0));
//            temp.add(new BlockStandard(Constants.DUMMY_BLOCK_ADDRESS, Util.sizedByteArrayWithInt(dummyCounter++, Constants.BLOCK_SIZE)));
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

    public List<BlockPath> decryptBlockPaths(List<BlockEncrypted> encryptedBlocks, boolean filterOutDummies) {
        List<BlockPath> res = new ArrayList<>();
        for (BlockEncrypted block : encryptedBlocks) {
//            System.out.println("Decrypt block");
            if (block == null) continue;
//            Address
            byte[] addressBytes = encryptionStrategy.decrypt(block.getAddress(), secretKey);

            if (addressBytes == null) continue;
            int addressInt = Util.byteArrayToLeInt(addressBytes);

            if (filterOutDummies && Util.isDummyAddress(addressInt)) continue;

//            Data and index
            byte[] encryptedDataFull = block.getData();

            int encryptedDataFullLength = encryptedDataFull.length;
            int endOfDataIndex = encryptedDataFullLength - Constants.AES_BLOCK_SIZE * 2;

            byte[] encryptedData = Arrays.copyOfRange(encryptedDataFull, 0, endOfDataIndex);
            byte[] encryptedIndex = Arrays.copyOfRange(encryptedDataFull, endOfDataIndex, encryptedDataFullLength);

            byte[] data = encryptionStrategy.decrypt(encryptedData, secretKey);
            byte[] indexBytes = encryptionStrategy.decrypt(encryptedIndex, secretKey);

            if (data == null || indexBytes == null) {
                Util.logAndPrint(logger, "Unable to decrypt a Path block");
                return null;
            }

            int index = Util.byteArrayToLeInt(indexBytes);

//            System.out.println("    Encrypted data full " + Arrays.toString(encryptedDataFull));
//            System.out.println("    Encrypted data full size " + encryptedDataFullLength);
//            System.out.println("    End of data index " + endOfDataIndex);
//            System.out.println("    Encrypted data " + Arrays.toString(encryptedData));
//            System.out.println("    Encrypted index " + Arrays.toString(encryptedIndex));
//            System.out.println("    Date " + Arrays.toString(data));
//            System.out.println("    Index bytes " + Arrays.toString(indexBytes));
//            System.out.println("    Index " + index);

            res.add(new BlockPath(addressInt, data, index));
        }
        return res;
    }

    public int getMaxStashSize() {
        return maxStashSize;
    }

    public int getMaxStashSizeBetweenAccesses() {
        return maxStashSizeBetweenAccesses;
    }
}
