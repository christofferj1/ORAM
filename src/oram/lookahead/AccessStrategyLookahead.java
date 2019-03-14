package oram.lookahead;


import javafx.util.Pair;
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
    public final SecretKey secretKey;
    private final CommunicationStrategy communicationStrategy;
    private final EncryptionStrategy encryptionStrategy;
    private final PermutationStrategy permutationStrategy;
    private Map<Integer, Index> positionMap;
    private int accessCounter;
    private List<SwapPartnerData> futureSwapPartners;


    public AccessStrategyLookahead(int size, int matrixHeight, byte[] key, Factory factory) {
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
//        First the matrix
        List<BlockLookahead> res = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            BlockLookahead block = blockLookaheads.get(i);
            Index index = block.getIndex();
            boolean isSwapPartner = futureSwapPartners.stream().anyMatch(s -> s.getIndex().equals(index));
            if (isSwapPartner)
                res.add(getLookaheadDummyBlock());
            else
                res.add(blockLookaheads.get(i));
        }
        futureSwapPartners = new ArrayList<>();

//        Then the access swap
        for (int i = size; i < size + matrixHeight; i++)
            res.add(getLookaheadDummyBlock());

//        At last the swap stash
        for (int i = (size + matrixHeight); i < (size + matrixHeight * 2); i++)
            res.add(swapPartners.get(i - (size + matrixHeight)));


//        Encrypt and write blocks to server
        List<BlockEncrypted> encryptedList = encryptBlocks(res);
        for (int i = 0; i < (size + matrixHeight * 2); i++) {
            if (!communicationStrategy.write(i, encryptedList.get(i)))
                return false;
        }

        System.out.println("Position map:");
        for (Integer i : positionMap.keySet())
            System.out.print(i + " -> " + positionMap.get(i) + ", ");
        System.out.println(" ");

        return true;
    }

    @Override
    public byte[] access(OperationType op, int address, byte[] data) {
        System.out.println("Going for address: " + address);
//        Fetch stashes
        Map<Integer, Map<Integer, BlockLookahead>> accessStash = getAccessStash();
        BlockLookahead[] swapStash = getSwapStash();

        System.out.println("Fetched access stash");
        for (Map.Entry<Integer, Map<Integer, BlockLookahead>> map : accessStash.entrySet()) {
            System.out.println("  Column: " + map.getKey());
            for (Map.Entry<Integer, BlockLookahead> entry : map.getValue().entrySet())
                System.out.println("    Row: " + entry.getKey() + ", block: \n    " + entry.getValue());
        }
        System.out.println("Fetches swap stash");
        for (BlockLookahead b : swapStash)
            System.out.print(b + ", ");
        System.out.println(" ");

        Index indexOfCurrentAddress = positionMap.get(address);
        System.out.println("Position: " + address + " -> " + indexOfCurrentAddress);
        BlockLookahead block = fetchBlockFromMatrix(indexOfCurrentAddress);
        System.out.println("Fetched block: \n" + block.toString());

        boolean blockFoundInMatrix = true;
        boolean blockFoundInAccessStash = true;
        int swapCount = 0;
        if (Util.isDummyAddress(block.getAddress())) {
            blockFoundInMatrix = false;
            block = findBlockInAccessStash(accessStash, indexOfCurrentAddress);
            if (block == null) {
                blockFoundInAccessStash = false;
                Pair<BlockLookahead, Integer> pair = findBlockInSwapStash(swapStash, address);
                if (pair == null) {
                    logger.error("Unable to locate block");
                    System.out.println("Unable to locate block");
                    return null;
                } else {
                    block = pair.getKey();
                    swapCount = pair.getValue();
                    logger.info("Block found in swap stash: " + block.toString());
                    System.out.println("Block found in swap stash: \n" + block.toString());
                    System.out.println("Swap count: " + swapCount);
                }
            } else {
                logger.info("Block found in access stash: " + block.toString());
                System.out.println("Block found in access stash: \n" + block.toString());
            }
        } else {
            logger.info("Block found in matrix: " + block.toString());
            System.out.println("Block found in matrix: \n" + block.toString());
        }

//        Get swap partner
        int accessCounterMod = Math.floorMod(accessCounter, matrixHeight);
        BlockLookahead swapPartner = swapStash[accessCounterMod];
        swapStash[accessCounterMod] = null;
        System.out.println("Swap access counter mod: " + accessCounterMod);
        System.out.println("Swap partner: " + swapPartner.toString());
        System.out.println("Swap stash after removing");
        for (BlockLookahead b : swapStash)
            System.out.print(b + ", ");
        System.out.println(" ");

//        Set index to index of swap partner and add to access stash
        block.setIndex(swapPartner.getIndex());
        accessStash = addToAccessStashMap(accessStash, block);
        System.out.println("New index for block read: " + block.getIndex().toString());
        System.out.println("Position map updated: " + address + " -> " + positionMap.get(address));

//        Update swap partner index and encrypt it
        swapPartner.setIndex(indexOfCurrentAddress);
        System.out.println("Swap partner new index: " + swapPartner.getIndex().toString());
        BlockEncrypted encryptedSwapPartner = encryptBlock(swapPartner);
        if (encryptedSwapPartner == null) return null;

//        Save data and overwrite if operation is a write
        byte[] res = block.getData();
        if (op.equals(OperationType.WRITE)) {block.setData(data);}

//        Update position map
        positionMap.put(swapPartner.getAddress(), swapPartner.getIndex());
        positionMap.put(block.getAddress(), block.getIndex());

        System.out.println("New access stash");
        for (Map.Entry<Integer, Map<Integer, BlockLookahead>> map : accessStash.entrySet()) {
            System.out.println("  Column: " + map.getKey());
            for (Map.Entry<Integer, BlockLookahead> entry : map.getValue().entrySet())
                System.out.println("    Row: " + entry.getKey() + ", block: \n    " + entry.getValue());
        }

        int flatArrayIndex = getFlatArrayIndex(indexOfCurrentAddress);
        if (blockFoundInMatrix) {
            if (!communicationStrategy.write(flatArrayIndex, encryptedSwapPartner)) {
                logger.error("Unable to write swap partner to communicationStrategy: \n" + swapPartner.toString());
                return null;
            }
            System.out.println("Simply but swap partner back into matrix");
        } else if (blockFoundInAccessStash) {
            if (!communicationStrategy.write(flatArrayIndex, encryptedSwapPartner)) {
                logger.error("Unable to write swap partner to communicationStrategy: \n" + swapPartner.toString());
                return null;
            }
            System.out.println("Removed old version from access stash");
//            Remove old version of block
            accessStash = removeFromAccessStash(accessStash, indexOfCurrentAddress);
        } else {
//            if (!communicationStrategy.write(flatArrayIndex, getEncryptedDummy(secretKey, encryptionStrategy))) {
            if (!communicationStrategy.write(flatArrayIndex, encryptBlock(getLookaheadDummyBlock()))) {
                logger.error("Unable to write swap partner to communicationStrategy: dummy block");
                return null;
            }
            System.out.println("Update swap stash");
            BlockLookahead swapReplacement = new BlockLookahead(swapPartner.getAddress(), swapPartner.getData());
            swapReplacement.setIndex(indexOfCurrentAddress);
            swapStash[swapCount] = swapReplacement;
            positionMap.put(swapReplacement.getAddress(), swapReplacement.getIndex());

            System.out.println("Swap stash after update");
            for (BlockLookahead b : swapStash)
                System.out.print(b + ", ");
            System.out.println(" ");
        }

        pickNewFutureSwapPartner(swapStash);
        if (!maintenanceJob(accessStash, swapStash))
            return null;

        accessCounter++;

        System.out.println("Position map:");
        for (Integer i : positionMap.keySet())
            System.out.print(i + " -> " + positionMap.get(i) + ", ");
        System.out.println(" ");

        return res;
    }

    boolean maintenanceJob(Map<Integer, Map<Integer, BlockLookahead>> accessStash, BlockLookahead[] swapStash) {
        System.out.println("\n   Maintenance job");
        int columnIndex = Math.floorMod(accessCounter, matrixHeight);
        System.out.println("Column index: " + columnIndex);
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
        System.out.println("Blocks retrieved");
        for (BlockLookahead b : column)
            System.out.println(b.toString());

//        Move blocks from access stash to column
        System.out.println("Move blocks from access stash");
        Map<Integer, BlockLookahead> map = accessStash.getOrDefault(columnIndex, new HashMap<>());
        for (Map.Entry<Integer, BlockLookahead> entry : map.entrySet()) {
            System.out.println("Row: " + entry.getKey() + ", block: \n" + entry.getValue());
            if (!Util.isDummyAddress(column.get(entry.getKey()).getAddress())) {
                logger.error("Was suppose to add accessed block to stash at index (" + entry.getKey() + ", " +
                        columnIndex + "), but place were not filled with dummy block");
                return false;
            }
            column.set(entry.getKey(), entry.getValue());
        }
        System.out.println("Column after access stash update");
        for (BlockLookahead b : column)
            System.out.println(b.toString());

        accessStash.remove(columnIndex);

//        Move blocks from column to swap stash
        System.out.println("Move blocks to swap stash");
        for (int i = futureSwapPartners.size() - 1; i >= 0; i--) {
            SwapPartnerData swap = futureSwapPartners.get(i);
            System.out.println(swap.toString());
            if (swap.getIndex().getColIndex() == columnIndex) {
                int rowIndex = swap.getIndex().getRowIndex();
                System.out.println("Placing at: (" + rowIndex + ", " + swap.getIndex().getColIndex() + ")");
                BlockLookahead swapPartner = column.get(rowIndex);
//                if (Util.isDummyAddress(swapPartner.getAddress())) { // TODO: this could actually be applied if #blocks = size, test that
//                    logger.error("Trying to set a dummy block as swap partner with swap data: " + swap);
//                    return false;
//                }
                swapStash[Math.floorMod(swap.getSwapNumber(), matrixHeight)] = swapPartner;
                futureSwapPartners.remove(i);
                column.set(rowIndex, getLookaheadDummyBlock());
            }
        }
        System.out.println("Column after swap stash update");
        for (BlockLookahead b : column)
            System.out.println(b.toString());
        System.out.println("Future swap partners after swap stash update");
        for (SwapPartnerData b : futureSwapPartners)
            System.out.println(b.toString());
        System.out.println("Swap stash after swap stash update");
        for (BlockLookahead b : swapStash)
            System.out.print(b == null ? null : b.toString() + ", ");
        System.out.println(" ");

//        Write back column
        List<BlockEncrypted> encryptedBlocks = encryptBlocks(column);
        if (encryptedBlocks.size() != matrixHeight) {
            logger.error("Wrong number of encrypted blocks to write back: " + encryptedBlocks.size());
        }
        for (int i = 0; i < matrixHeight; i++) {
            Index index = new Index(i, columnIndex);
            if (!communicationStrategy.write(getFlatArrayIndex(index), encryptedBlocks.get(i))) {
                logger.error("Unable to write block to column with index: " + index);
                System.out.println("Unable to write block to column with index: " + index);
                return false;
            }
        }

//        Write back access stash
        encryptedBlocks = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, BlockLookahead>> innerMap : accessStash.entrySet()) {
            for (Map.Entry<Integer, BlockLookahead> entry : innerMap.getValue().entrySet())
                encryptedBlocks.add(encryptBlock(entry.getValue()));
        }
        for (int i = 0; i < matrixHeight; i++) {
            int index = size + i;
            BlockEncrypted block;
            if (encryptedBlocks.size() <= i)
                block = encryptBlock(getLookaheadDummyBlock());
//                block = Util.getEncryptedDummy(secretKey, encryptionStrategy);
            else
                block = encryptedBlocks.get(i);

            if (!communicationStrategy.write(index, block)) {
                logger.error("Unable to write block to access stash with index: " + index);
                System.out.println("Unable to write block to access stash with index: " + index);
                return false;
            }
        }

//        Write back swap stash
        encryptedBlocks = new ArrayList<>();
        for (BlockLookahead block : swapStash) {
            if (block == null)
                encryptedBlocks.add(encryptBlock(getLookaheadDummyBlock()));
//                encryptedBlocks.add(Util.getEncryptedDummy(secretKey, encryptionStrategy));
            else
                encryptedBlocks.add(encryptBlock(block));
        }
        for (int i = 0; i < matrixHeight; i++) {
            int index = size + matrixHeight + i;
            if (!communicationStrategy.write(index, encryptedBlocks.get(i))) {
                logger.error("Unable to write block to swap stash with index: " + index);
                System.out.println("Unable to write block to swap stash with index: " + index);
                return false;
            }
        }

        return true;
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
            System.out.println("Removed from access stash: " + map.get(index.getRowIndex()));
            map.remove(index.getRowIndex());
            if (map.isEmpty()) {
                stash.remove(index.getColIndex());
                System.out.println("Entire column removed");
            }
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

    BlockLookahead findBlockInAccessStash(Map<Integer, Map<Integer, BlockLookahead>> stash, Index index) {
        int colIndex = index.getColIndex();
        int rowIndex = index.getRowIndex();
        if (stash.containsKey(colIndex)) {
            Map<Integer, BlockLookahead> columnMap = stash.get(colIndex);
            BlockLookahead res = columnMap.getOrDefault(rowIndex, null);
            if (res == null || Util.isDummyAddress(res.getAddress())) return null;
            return res;
        }
        return null;
    }

    private Pair<BlockLookahead, Integer> findBlockInSwapStash(BlockLookahead[] stash, int address) {
        for (int i = 0; i < stash.length; i++) {
            BlockLookahead block = stash[i];
            if (block != null && block.getAddress() == address)
                return new Pair<>(block, i);
        }
        return null;
    }

    private void pickNewFutureSwapPartner(BlockLookahead[] swapStash) {
        List<BlockLookahead> swapStashList = Arrays.asList(swapStash);
        System.out.println("Current future swap partners");
        for (SwapPartnerData s : futureSwapPartners)
            System.out.println(s.toString());
        SecureRandom randomness = new SecureRandom();
        boolean futureSwapsContainsIndex = true;
        boolean swapStashContains = true;
        Index index = null;
        System.out.print("Possible future swap indices: ");
        while (futureSwapsContainsIndex || swapStashContains) {
            index = new Index(randomness.nextInt(matrixHeight), randomness.nextInt(matrixHeight));
            Index finalIndex = index;
            System.out.print(index.toString() + ", ");
            futureSwapsContainsIndex = futureSwapPartners.stream().anyMatch(f -> f.getIndex().equals(finalIndex));
            swapStashContains = swapStashList.stream().anyMatch(s -> (s != null && s.getIndex().equals(finalIndex)));
        }
        System.out.println(" ");
        futureSwapPartners.add(new SwapPartnerData(index, accessCounter));
        System.out.println("New future swap partners");
        for (SwapPartnerData s : futureSwapPartners)
            System.out.println(s.toString());
    }

    public BlockLookahead decryptToLookaheadBlock(BlockEncrypted blockEncrypted) {
//        System.out.println("Encrypting with secret key: " + Arrays.toString(secretKey.getEncoded()));

        byte[] encryptedDataFull = blockEncrypted.getData();
        int encryptedDataFullLength = encryptedDataFull.length;
        int endOfDataIndex = encryptedDataFullLength - Constants.BLOCK_SIZE * 2;
        byte[] encryptedData = Arrays.copyOfRange(encryptedDataFull, 0, endOfDataIndex);
        byte[] encryptedIndex = Arrays.copyOfRange(encryptedDataFull, endOfDataIndex, encryptedDataFullLength);
        byte[] data = encryptionStrategy.decrypt(encryptedData, secretKey);
        byte[] indices = encryptionStrategy.decrypt(encryptedIndex, secretKey);
        if (data == null) {
            logger.info("Tried to turn an encrypted block with value = null into a Lookahead block");
            return null;
        }

        byte[] addressBytes = encryptionStrategy.decrypt(blockEncrypted.getAddress(), secretKey);
        if (addressBytes == null || addressBytes.length < 4 || indices == null || indices.length < 8) {
            System.out.println("What a pickle");
            return null;
        }
        int address = byteArrayToLeInt(addressBytes);

        int rowDataIndex = 0;
        int colDataIndex = INTEGER_BYTE_ARRAY_SIZE;
//        byte[] blockData = Arrays.copyOfRange(data, 0, rowDataIndex);
        byte[] rowIndexBytes = Arrays.copyOfRange(indices, rowDataIndex, colDataIndex);
        byte[] colIndexBytes = Arrays.copyOfRange(indices, colDataIndex, INTEGER_BYTE_ARRAY_SIZE * 2);

        BlockLookahead blockLookahead = new BlockLookahead();
        blockLookahead.setAddress(address);
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
//        System.out.println("Encrypting with secret key: " + Arrays.toString(secretKey.getEncoded()));

        List<BlockEncrypted> res = new ArrayList<>();
        for (BlockLookahead block : blockLookaheads) {
            if (block == null) {
                res.add(null);
                continue;
            }
            byte[] rowIndexBytes = Util.leIntToByteArray(block.getRowIndex());
            byte[] colIndexBytes = Util.leIntToByteArray(block.getColIndex());
            byte[] addressBytes = Util.leIntToByteArray(block.getAddress());
            byte[] encryptedAddress = encryptionStrategy.encrypt(addressBytes, secretKey);
            byte[] encryptedData = encryptionStrategy.encrypt(block.getData(), secretKey);
            byte[] encryptedIndex = encryptionStrategy.encrypt(ArrayUtils.addAll(rowIndexBytes, colIndexBytes),
                    secretKey);
            byte[] encryptedDataPlus = ArrayUtils.addAll(encryptedData, encryptedIndex);
            if (encryptedDataPlus.length < 64)
                System.out.println("Something fishy is going on");
            res.add(new BlockEncrypted(encryptedAddress, encryptedDataPlus));
//            System.out.println("Block to encrypt: " + block.toString());
//            System.out.println("  Address bytes: " + Arrays.toString(addressBytes));
//            System.out.println("  Encrypted address: " + Arrays.toString(encryptedAddress));
//            System.out.println("  Encrypted data: " + Arrays.toString(encryptedData));
//            System.out.println("  Encrypted index: " + Arrays.toString(encryptedIndex));
        }
        return res;
    }

    int getFlatArrayIndex(Index index) {
        int res = index.getRowIndex();
        res += index.getColIndex() * matrixHeight;
        return res;
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

    BlockLookahead getLookaheadDummyBlock() {
        BlockLookahead blockLookahead = new BlockLookahead(0, new byte[Constants.BLOCK_SIZE]);
        blockLookahead.setIndex(new Index(0, 0));
        return blockLookahead;
    }
}
