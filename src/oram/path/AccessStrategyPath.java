package oram.path;

import oram.AccessStrategy;
import oram.Constants;
import oram.OperationType;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockPath;
import oram.block.BlockTrivial;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.factory.Factory;
import oram.permutation.PermutationStrategy;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.*;

import static oram.Constants.DUMMY_LEAF_NODE_INDEX;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 19-02-2019. <br>
 */

public class AccessStrategyPath implements AccessStrategy {
    private static final Logger logger = LogManager.getLogger("log");
    private final int size;
    private final int L;
    private final int bucketSize;
    private final int leafCount;
    private final int offset;
    private final SecretKey secretKey;
    private final CommunicationStrategy communicationStrategy;
    private final EncryptionStrategy encryptionStrategy;
    private final PermutationStrategy permutationStrategy;
    private int maxStashSize;
    private int maxStashSizeBetweenAccesses;
    private List<BlockPath> stash;
    private Map<Integer, Integer> positionMap;
    private AccessStrategy accessStrategy;
    private String prefixString;

    public AccessStrategyPath(int size, int bucketSize, byte[] key, Factory factory, AccessStrategy accessStrategy,
                              int offset, int prefixSize) {
        this.size = size;
        this.bucketSize = bucketSize;
        this.offset = offset;
        stash = new ArrayList<>();
        L = (int) Math.ceil(Math.log(size) / Math.log(2));
        leafCount = (int) (Math.pow(2, L - 1));
        communicationStrategy = factory.getCommunicationStrategy();
        encryptionStrategy = factory.getEncryptionStrategy();
        secretKey = encryptionStrategy.generateSecretKey(key);
        permutationStrategy = factory.getPermutationStrategy();
        maxStashSize = 0;
        maxStashSizeBetweenAccesses = 0;

        prefixString = Util.getEmptyStringOfLength(prefixSize);

        if (accessStrategy != null)
            this.accessStrategy = accessStrategy;
        else
            positionMap = new HashMap<>();

        logger.info("######### Initialized Path ORAM strategy #########");
        logger.debug("######### Initialized Path ORAM strategy #########");
    }

    public void setup() { // For tests only
        double numberOfLeaves = Math.pow(2, L - 1);
        for (int i = 0; i < numberOfLeaves; i++) {
            positionMap.put(0, i);
            access(OperationType.WRITE, 0, new byte[Constants.BLOCK_SIZE], false, false);
        }
        positionMap = new HashMap<>();
    }

    @Override
    public boolean setup(List<BlockTrivial> blocks) {
        return true;
    }

    @Override
    public byte[] access(OperationType op, int address, byte[] data, boolean recursiveLookup, boolean lookaheadSetup) {
        if (data != null && data.length > Constants.BLOCK_SIZE) {
            logger.error(prefixString + "Accessed with data length: " + data.length);
        }

        int addressToLookUp = address;
        if (recursiveLookup)
            addressToLookUp = (int) Math.ceil((double) address / Constants.POSITION_BLOCK_SIZE);

        SecureRandom randomness = new SecureRandom();

//        Line 1 and 2 in pseudo code.
//        Return a random position if the block does not have one already
        Integer leafNodeIndex;
        Integer newLeafNodeIndex = randomness.nextInt((int) (Math.pow(2, L - 1)));
        if (positionMap == null) {
            Map<Integer, Integer> positionMap = Util.getPositionMap(addressToLookUp, newLeafNodeIndex, accessStrategy);
            if (positionMap == null)
                return null;

            leafNodeIndex = positionMap.getOrDefault(addressToLookUp, null);

            if (leafNodeIndex == null) {
                logger.error(prefixString + "Unable to look up address: " + address);
                return null;
            } else if (leafNodeIndex == DUMMY_LEAF_NODE_INDEX)
                leafNodeIndex = randomness.nextInt((int) Math.pow(2, L - 1));

        } else {
            if (positionMap.containsKey(addressToLookUp))
                leafNodeIndex = positionMap.get(addressToLookUp);
            else
                leafNodeIndex = randomness.nextInt((int) (Math.pow(2, L - 1)));

            positionMap.put(addressToLookUp, newLeafNodeIndex);
        }

        logger.info(prefixString + "Access op: " + op.toString() + ", address: " + addressToLookUp +
                ", leaf node: " + leafNodeIndex + " -> " + newLeafNodeIndex);

//        Line 3 to 5 in pseudo code.
        boolean readPath = readPathToStash(leafNodeIndex);
        if (!readPath) {
            logger.error(prefixString + "Unable to read path doing access");
            return null;
        }

//        Line 6 to 9 in pseudo code
        byte[] res = retrieveDataOverwriteBlock(address, op, data, newLeafNodeIndex, recursiveLookup, addressToLookUp);
        if (res == null) {
            logger.error(prefixString + "Something went wrong, when getting data from the block with address: " + address);
            return null;
        }
        if (Arrays.equals(res, new byte[0])) {
            logger.error(prefixString + "Unable to retrieve data from address: " + addressToLookUp + (recursiveLookup ? ", create dummy lookup map" : ""));
            if (recursiveLookup)
                res = Util.getByteArrayFromMap(Util.getDummyMap(address));
        }


//        Line 10 to 15 in pseudo code.
        boolean writeBack = writeBackPath(leafNodeIndex);
        if (!writeBack) {
            logger.error(prefixString + "Unable to write back path with doing access");
            return null;
        }

        if (stash.size() > maxStashSizeBetweenAccesses) {
            maxStashSizeBetweenAccesses = stash.size();
            logger.info(prefixString + "Max stash size between accesses: " + maxStashSizeBetweenAccesses);
        }

   return res;
    }

    private boolean readPathToStash(int leafNodeIndex) {
        boolean res = true;
        List<Integer> positionsToRead = new ArrayList<>();
        for (int l = 0; l < L; l++) {
            int nodeNumber = getNode(leafNodeIndex, l);
            int position = nodeNumber * bucketSize;

           for (int i = 0; i < bucketSize; i++)
                positionsToRead.add(position + i + offset);
        }

        List<BlockEncrypted> encryptedBlocks = communicationStrategy.readArray(positionsToRead);

        if (encryptedBlocks == null || bucketSize * L != encryptedBlocks.size()) {
            logger.error(prefixString + "Did not fetch the right amount of blocks");
            res = false;
        } else {
            List<BlockPath> blocksDecrypted = decryptBlockPaths(encryptedBlocks, true);
            if (blocksDecrypted == null) {
                logger.error(prefixString + "Unable to decrypt path of blocks");
                res = false;
            } else {
                                stash.addAll(blocksDecrypted);
                if (stash.size() > maxStashSize) {
                    maxStashSize = stash.size();
                    logger.info(prefixString + "Max stash size: " + maxStashSize);
                }
            }
        }
        return res;
    }

    /**
     * As their might not be a block to overwrite, we keep track of whether or not any block has been overwritten
     *
     * @param data if recursiveLookup is true, data contains the newLeafLookupIndex for the parent ORAM
     * @return the data the access must return at last
     */
    private byte[] retrieveDataOverwriteBlock(int address, OperationType op, byte[] data, int newLeafNodeIndex,
                                              boolean recursiveLookup, int addressToLookUp) {
        boolean hasOverwrittenBlock = false;
        byte[] endData = new byte[0];
        for (int i = 0; i < stash.size(); i++) {
            if (stash.get(i).getAddress() == addressToLookUp) {
                endData = stash.get(i).getData();
                if (op.equals(OperationType.WRITE)) {
                    if (recursiveLookup) {
                        Map<Integer, Integer> map = Util.getMapFromByteArray(endData);

                        if (map == null)
                            return null;
                        map.put(address, Util.byteArrayToLeInt(data));
                        stash.set(i, new BlockPath(addressToLookUp, Util.getByteArrayFromMap(map), newLeafNodeIndex));
                    } else {
                        stash.set(i, new BlockPath(addressToLookUp, data, newLeafNodeIndex));
                    }
                    hasOverwrittenBlock = true;
                } else
                    stash.get(i).setIndex(newLeafNodeIndex); // Update leaf node index, even for op = READ
                break;
            }
        }

        if (op.equals(OperationType.WRITE) && !hasOverwrittenBlock) {
            if (recursiveLookup) {
//                Create new dummy map
                Map<Integer, Integer> map = Util.getDummyMap(address);
//                Add the map to the block, which is added to the stash
                if (map == null)
                    return null;
                map.put(address, Util.byteArrayToLeInt(data));
                stash.add(new BlockPath(addressToLookUp, Util.getByteArrayFromMap(map), newLeafNodeIndex));
            } else
                stash.add(new BlockPath(addressToLookUp, data, newLeafNodeIndex));
        }
        if (stash.size() > maxStashSize) {
            maxStashSize = stash.size();
            logger.info(prefixString + "Max stash size: " + maxStashSize);
        }

        return endData;
    }

    private boolean writeBackPath(int leafNode) {
             List<Integer> addressesToWrite = new ArrayList<>();
        List<BlockEncrypted> encryptedBlocksToWrite = new ArrayList<>();
        for (int l = L - 1; l >= 0; l--) {
            int nodeNumber = getNode(leafNode, l);
            int arrayPosition = nodeNumber * bucketSize;

//            Pick all the blocks from the stash which can be written to the current node
            List<BlockPath> blocksToWrite = getBlocksForNode(nodeNumber);

//            Make sure there are exactly Z blocks to write to the node
            blocksToWrite = fillBucketWithDummyBlocks(blocksToWrite);
            blocksToWrite = permutationStrategy.permutePathBlocks(blocksToWrite);

//            Remove the blocks from the stash, before they are written to the node
            removeBlocksFromStash(blocksToWrite);

//            Encrypts all pairs
            List<BlockEncrypted> encryptedBlocksToWriteTmp = encryptBucketOfBlocks(blocksToWrite);
            if (encryptedBlocksToWriteTmp == null) {
                logger.error(prefixString + "Returned null when trying to encrypt blocks");
                return false;
            }
            for (int i = 0; i < blocksToWrite.size(); i++) {
                addressesToWrite.add(arrayPosition + i + offset);
                encryptedBlocksToWrite.add(encryptedBlocksToWriteTmp.get(i));
            }
        }

        if (!communicationStrategy.writeArray(addressesToWrite, encryptedBlocksToWrite)) {
            logger.error(prefixString + "Writing returned unsuccessful");
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
                logger.error(prefixString + "Unable to encrypt address: " + block.getAddress() + " or data");
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
                Util.logAndPrint(logger, prefixString + "Unable to decrypt a Path block");
                return null;
            }

            int index = Util.byteArrayToLeInt(indexBytes);

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
