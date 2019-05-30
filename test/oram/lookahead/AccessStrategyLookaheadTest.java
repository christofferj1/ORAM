package oram.lookahead;

import oram.Constants;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockLookahead;
import oram.block.BlockTrivial;
import oram.blockenc.BlockEncryptionStrategyLookahead;
import oram.clientcom.CommunicationStrategyStub;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyImpl;
import oram.util.FactoryStub;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.util.*;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertNull;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 04-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class AccessStrategyLookaheadTest {
    private AccessStrategyLookahead access;
    private byte[] defaultKey;
    private SecretKey secretKey;
    private int defaultSize;
    private int defaultMatrixSize;
    private FactoryStub factory;
    private BlockEncryptionStrategyLookahead blockEncStrategyLookahead;

    @Before
    public void setUp() {
        defaultKey = "Some key".getBytes();
        defaultSize = 16;
        defaultMatrixSize = 4;
        factory = new FactoryStub(new CommunicationStrategyStub(0, 0));
        secretKey = factory.getEncryptionStrategy().generateSecretKey(defaultKey);
        access = new AccessStrategyLookahead(defaultSize, defaultMatrixSize, defaultKey, factory, 0, null, 0);
        blockEncStrategyLookahead = new BlockEncryptionStrategyLookahead(factory.getEncryptionStrategy());
    }

    @Test
    public void shouldBeAbleToConvertTrivialBlocksToLookaheadBlocks() {
        BlockTrivial block1 = new BlockTrivial(11, "Block 1".getBytes());
        BlockTrivial block2 = new BlockTrivial(2, "Block 2".getBytes());
        BlockTrivial block3 = new BlockTrivial(8, "Block 3".getBytes());
        BlockTrivial block4 = new BlockTrivial(15, "Block 4".getBytes());
        BlockTrivial block5 = new BlockTrivial(4, "Block 5".getBytes());
        BlockTrivial block6 = new BlockTrivial(6, "Block 6".getBytes());
        BlockTrivial block7 = new BlockTrivial(13, "Block 7".getBytes());
        BlockTrivial block8 = new BlockTrivial(3, "Block 8".getBytes());
        BlockTrivial block9 = new BlockTrivial(9, "Block 9".getBytes());
        BlockTrivial block10 = new BlockTrivial(10, "Block 10".getBytes());
        BlockTrivial block11 = new BlockTrivial(1, "Block 11".getBytes());
        BlockTrivial block12 = new BlockTrivial(12, "Block 12".getBytes());
        BlockTrivial block13 = new BlockTrivial(7, "Block 13".getBytes());
        BlockTrivial block14 = new BlockTrivial(14, "Block 14".getBytes());
        BlockTrivial block15 = new BlockTrivial(5, "Block 15".getBytes());
        BlockTrivial block16 = new BlockTrivial(0, new byte[Constants.BLOCK_SIZE]);
        List<BlockTrivial> blocks = new ArrayList<>(Arrays.asList(block1, block2, block3, block4, block5, block6,
                block7, block8, block9, block10, block11, block12, block13, block14, block15, block16));

        List<BlockLookahead> res = access.trivialToLookaheadBlocksForSetup(blocks);
        assertThat(res, hasSize(16));
        assertThat(res.get(0), is(new BlockLookahead(block1.getAddress(), block1.getData(), 0, 0)));
        assertThat(res.get(1), is(new BlockLookahead(block2.getAddress(), block2.getData(), 1, 0)));
        assertThat(res.get(2), is(new BlockLookahead(block3.getAddress(), block3.getData(), 2, 0)));
        assertThat(res.get(3), is(new BlockLookahead(block4.getAddress(), block4.getData(), 3, 0)));
        assertThat(res.get(4), is(new BlockLookahead(block5.getAddress(), block5.getData(), 0, 1)));
        assertThat(res.get(5), is(new BlockLookahead(block6.getAddress(), block6.getData(), 1, 1)));
        assertThat(res.get(6), is(new BlockLookahead(block7.getAddress(), block7.getData(), 2, 1)));
        assertThat(res.get(7), is(new BlockLookahead(block8.getAddress(), block8.getData(), 3, 1)));
        assertThat(res.get(8), is(new BlockLookahead(block9.getAddress(), block9.getData(), 0, 2)));
        assertThat(res.get(9), is(new BlockLookahead(block10.getAddress(), block10.getData(), 1, 2)));
        assertThat(res.get(10), is(new BlockLookahead(block11.getAddress(), block11.getData(), 2, 2)));
        assertThat(res.get(11), is(new BlockLookahead(block12.getAddress(), block12.getData(), 3, 2)));
        assertThat(res.get(12), is(new BlockLookahead(block13.getAddress(), block13.getData(), 0, 3)));
        assertThat(res.get(13), is(new BlockLookahead(block14.getAddress(), block14.getData(), 1, 3)));
        assertThat(res.get(14), is(new BlockLookahead(block15.getAddress(), block15.getData(), 2, 3)));
        assertThat(res.get(15), is(new BlockLookahead(block16.getAddress(), block16.getData(), 3, 3)));
    }

    @Test
    public void shouldBeAbleToCreateAccessStash() {
        setStandardServer();

        BlockLookahead block0 = new BlockLookahead(1, Util.leIntToByteArray(0), 0, 0);
        BlockLookahead block1 = new BlockLookahead(2, Util.leIntToByteArray(1), 1, 0);
        BlockLookahead block2 = new BlockLookahead(3, Util.leIntToByteArray(2), 2, 0);
        BlockLookahead block3 = new BlockLookahead(4, Util.leIntToByteArray(3), 3, 0);
        BlockLookahead block4 = new BlockLookahead(5, Util.leIntToByteArray(4), 0, 1);
        BlockLookahead block7 = new BlockLookahead(8, Util.leIntToByteArray(7), 3, 1);
        BlockLookahead block8 = new BlockLookahead(9, Util.leIntToByteArray(8), 0, 2);
        BlockLookahead block9 = new BlockLookahead(10, Util.leIntToByteArray(9), 1, 2);
        BlockLookahead block10 = new BlockLookahead(11, Util.leIntToByteArray(10), 2, 2);
        BlockLookahead block12 = new BlockLookahead(13, Util.leIntToByteArray(12), 0, 3);
        BlockLookahead block14 = new BlockLookahead(15, Util.leIntToByteArray(14), 2, 3);

        List<BlockLookahead> blocks = Arrays.asList(block1, block2, block4, block7, block0, block3, block10, block12);

//        First we try where the wanted cell was in the column we read
        Map<Integer, Map<Integer, BlockLookahead>> map = access.getAccessStash(blocks, true);
//        It should have three entries, where the first is a map with two entries
        assertThat(map, aMapWithSize(3));
        assertThat(map, hasKey(0));
        assertThat(map, hasKey(2));
        assertThat(map, hasKey(3));

        Map<Integer, BlockLookahead> map0 = map.get(0);
        assertThat(map0, aMapWithSize(2));
        assertThat(map0, hasEntry(0, new BlockLookahead(1, Util.leIntToByteArray(0), 0, 0)));
        assertThat(map0, hasEntry(3, new BlockLookahead(4, Util.leIntToByteArray(3), 3, 0)));

        Map<Integer, BlockLookahead> map2 = map.get(2);
        assertThat(map2, aMapWithSize(1));
        assertThat(map2, hasEntry(2, new BlockLookahead(11, Util.leIntToByteArray(10), 2, 2)));

        Map<Integer, BlockLookahead> map3 = map.get(3);
        assertThat(map3, aMapWithSize(1));
        assertThat(map3, hasEntry(0, new BlockLookahead(13, Util.leIntToByteArray(12), 0, 3)));

//        Then we try where the wanted cell was not in the column we read
        blocks = Arrays.asList(block8, block1, block2, block4, block7, block0, block3, block10, block12);


        map = access.getAccessStash(blocks, false);
//        It should have three entries, where the first is a map with two entries
        assertThat(map, aMapWithSize(3));
        assertThat(map, hasKey(0));
        assertThat(map, hasKey(2));
        assertThat(map, hasKey(3));

        map0 = map.get(0);
        assertThat(map0, aMapWithSize(2));
        assertThat(map0, hasEntry(0, new BlockLookahead(1, Util.leIntToByteArray(0), 0, 0)));
        assertThat(map0, hasEntry(3, new BlockLookahead(4, Util.leIntToByteArray(3), 3, 0)));

        map2 = map.get(2);
        assertThat(map2, aMapWithSize(1));
        assertThat(map2, hasEntry(2, new BlockLookahead(11, Util.leIntToByteArray(10), 2, 2)));

        map3 = map.get(3);
        assertThat(map3, aMapWithSize(1));
        assertThat(map3, hasEntry(0, new BlockLookahead(13, Util.leIntToByteArray(12), 0, 3)));
    }

    @Test
    public void shouldBeAbleToCreateSwatStash() {
        setStandardServer();

        BlockLookahead block0 = new BlockLookahead(1, Util.leIntToByteArray(0), 0, 0);
        BlockLookahead block1 = new BlockLookahead(2, Util.leIntToByteArray(1), 1, 0);
        BlockLookahead block2 = new BlockLookahead(3, Util.leIntToByteArray(2), 2, 0);
        BlockLookahead block4 = new BlockLookahead(5, Util.leIntToByteArray(4), 0, 1);
        BlockLookahead block10 = new BlockLookahead(11, Util.leIntToByteArray(10), 2, 3);
        BlockLookahead block11 = new BlockLookahead(12, Util.leIntToByteArray(11), 3, 4);
        BlockLookahead block12 = new BlockLookahead(13, Util.leIntToByteArray(12), 4, 5);
        BlockLookahead block13 = new BlockLookahead(14, Util.leIntToByteArray(13), 5, 6);
        BlockLookahead block3 = new BlockLookahead(4, Util.leIntToByteArray(3), 3, 0);
        BlockLookahead block7 = new BlockLookahead(8, Util.leIntToByteArray(7), 3, 1);
        BlockLookahead block8 = new BlockLookahead(9, Util.leIntToByteArray(8), 0, 2);
        BlockLookahead block9 = new BlockLookahead(10, Util.leIntToByteArray(9), 1, 2);
        BlockLookahead block14 = new BlockLookahead(15, Util.leIntToByteArray(14), 2, 3);

        List<BlockLookahead> blocks = Arrays.asList(block0, block1, block2, block3, block4, block10, block11, block12,
                block7, block9, block8, block14);

        BlockLookahead[] list = access.getSwapStash(blocks, true);

        assertThat(list, arrayWithSize(4));
        assertThat(list, hasItemInArray(new BlockLookahead(8, Util.leIntToByteArray(7), 3, 1)));
        assertThat(list, hasItemInArray(new BlockLookahead(9, Util.leIntToByteArray(8), 0, 2)));
        assertThat(list, hasItemInArray(new BlockLookahead(10, Util.leIntToByteArray(9), 1, 2)));
        assertThat(list, hasItemInArray(new BlockLookahead(15, Util.leIntToByteArray(14), 2, 3)));

        blocks = Arrays.asList(block0, block1, block2, block3, block4, block10, block11, block12,
                block13, block7, block9, block8, block14);

        list = access.getSwapStash(blocks, false);

        assertThat(list, arrayWithSize(4));
        assertThat(list, hasItemInArray(new BlockLookahead(8, Util.leIntToByteArray(7), 3, 1)));
        assertThat(list, hasItemInArray(new BlockLookahead(9, Util.leIntToByteArray(8), 0, 2)));
        assertThat(list, hasItemInArray(new BlockLookahead(10, Util.leIntToByteArray(9), 1, 2)));
        assertThat(list, hasItemInArray(new BlockLookahead(15, Util.leIntToByteArray(14), 2, 3)));
    }

    @Test
    public void shouldAddTheBlocksCorrectlyToTheStashMap() {
//        Adding first block
        BlockLookahead block = new BlockLookahead(17, new byte[]{32});
        block.setRowIndex(4);
        block.setColIndex(3);
        Map<Integer, Map<Integer, BlockLookahead>> value = access.addToAccessStashMap(new HashMap<>(), block);
        assertThat("Outer map should include col 3", value, hasKey(3));
        assertThat("Inner map should include row 4", value.get(3), hasEntry(4, block));
        assertThat("Outer map has 1 element", value, aMapWithSize(1));
        assertThat("Inner map 3 has 1 element", value.get(3), aMapWithSize(1));

//        Adding block to same column (just extending the inner map)
        block.setRowIndex(1);
        block.setColIndex(3);
        value = access.addToAccessStashMap(value, block);
        assertThat("Outer map should include col 3", value, hasKey(3));
        assertThat("Inner map should include row 4", value.get(3), hasEntry(4, block));
        assertThat("Inner map should include row 1", value.get(3), hasEntry(1, block));
        assertThat("Outer map has 1 element", value, aMapWithSize(1));
        assertThat("Inner map 3 has 2 elements", value.get(3), aMapWithSize(2));

//        Adding block to a new column
        block.setRowIndex(4);
        block.setColIndex(1);
        value = access.addToAccessStashMap(value, block);
        assertThat("Outer map should include col 3", value, hasKey(3));
        assertThat("Outer map should include col 1", value, hasKey(1));
        assertThat("Inner map should include row 4", value.get(3), hasEntry(4, block));
        assertThat("Inner map should include row 1", value.get(3), hasEntry(1, block));
        assertThat("Inner map should include row 4", value.get(1), hasEntry(4, block));
        assertThat("Outer map has 2 element", value, aMapWithSize(2));
        assertThat("Inner map 3 has 2 elements", value.get(3), aMapWithSize(2));
        assertThat("Inner map 1 has 1 element", value.get(1), aMapWithSize(1));
    }

    @Test
    public void shouldBeAbleToLookUpInAccessStash() {
        BlockLookahead block0 = new BlockLookahead(5, null);
        BlockLookahead block1 = new BlockLookahead(1, null);
        BlockLookahead block2 = new BlockLookahead(2, null);
        BlockLookahead block3 = new BlockLookahead(3, null);
        BlockLookahead block4 = new BlockLookahead(4, null);

        Map<Integer, BlockLookahead> map0 = new HashMap<>();
        map0.put(3, block0);

        Map<Integer, BlockLookahead> map1 = new HashMap<>();
        map1.put(0, block1);
        map1.put(1, block2);

        Map<Integer, BlockLookahead> map3 = new HashMap<>();
        map3.put(0, block3);
        map3.put(2, block4);

        Map<Integer, Map<Integer, BlockLookahead>> map = new HashMap<>();
        map.put(0, map0);
        map.put(1, map1);
        map.put(3, map3);

//        Should be able to look up the blocks in the map
        assertThat(access.findBlockInAccessStash(map, new Index(3, 0)), is(block0));
        assertThat(access.findBlockInAccessStash(map, new Index(0, 1)), is(block1));
        assertThat(access.findBlockInAccessStash(map, new Index(1, 1)), is(block2));
        assertThat(access.findBlockInAccessStash(map, new Index(0, 3)), is(block3));
        assertThat(access.findBlockInAccessStash(map, new Index(2, 3)), is(block4));

//        All other should return null
        assertNull(access.findBlockInAccessStash(map, new Index(0, 0)));
        assertNull(access.findBlockInAccessStash(map, new Index(1, 0)));
        assertNull(access.findBlockInAccessStash(map, new Index(2, 0)));
        assertNull(access.findBlockInAccessStash(map, new Index(2, 1)));
        assertNull(access.findBlockInAccessStash(map, new Index(3, 1)));
        assertNull(access.findBlockInAccessStash(map, new Index(0, 2)));
        assertNull(access.findBlockInAccessStash(map, new Index(1, 2)));
        assertNull(access.findBlockInAccessStash(map, new Index(2, 2)));
        assertNull(access.findBlockInAccessStash(map, new Index(3, 2)));
        assertNull(access.findBlockInAccessStash(map, new Index(1, 3)));
        assertNull(access.findBlockInAccessStash(map, new Index(3, 3)));
    }

    @Test
    public void shouldBeAbleToGetLookaheadBlockFromEncryptedBlock() {
        byte[] blockData = Util.getRandomByteArray(10);
        int addressInt = 42;
        int rowIndex = 133742;
        int colIndex = 0;

//        Define method specific encryption
        EncryptionStrategy encryptionStrategy = new EncryptionStrategyImpl();
        SecretKey secretKey = encryptionStrategy.generateSecretKey(defaultKey);
        factory.setEncryptionStrategy(encryptionStrategy);
        access = new AccessStrategyLookahead(defaultSize, defaultMatrixSize, defaultKey, factory, 0, null, 0);

        byte[] rowBytes = Util.leIntToByteArray(rowIndex);
        byte[] colBytes = Util.leIntToByteArray(colIndex);
        byte[] encryptedIndex = encryptionStrategy.encrypt(ArrayUtils.addAll(rowBytes, colBytes), secretKey);
        byte[] encryptedData = encryptionStrategy.encrypt(blockData, secretKey);
        byte[] address = Util.leIntToByteArray(addressInt);
        byte[] encryptedAddress = encryptionStrategy.encrypt(address, secretKey);

        BlockEncrypted blockEncrypted = new BlockEncrypted(encryptedAddress,
                ArrayUtils.addAll(encryptedData, encryptedIndex));

        BlockLookahead blockLookahead = blockEncStrategyLookahead.decryptBlock(blockEncrypted, secretKey);
        assertThat("Correct data", blockLookahead.getData(), is(blockData));
        assertThat("Correct address", blockLookahead.getAddress(), is(addressInt));
        assertThat("Correct row index", blockLookahead.getRowIndex(), is(rowIndex));
        assertThat("Correct col index", blockLookahead.getColIndex(), is(colIndex));

        blockData = Util.getRandomByteArray(1000);
        addressInt = 0;
        rowIndex = 13312742;
        colIndex = 2121230;

        rowBytes = Util.leIntToByteArray(rowIndex);
        colBytes = Util.leIntToByteArray(colIndex);
        encryptedIndex = encryptionStrategy.encrypt(ArrayUtils.addAll(rowBytes, colBytes), secretKey);
        encryptedData = encryptionStrategy.encrypt(blockData, secretKey);
        address = Util.leIntToByteArray(addressInt);
        encryptedAddress = encryptionStrategy.encrypt(address, secretKey);

        blockEncrypted = new BlockEncrypted(encryptedAddress,
                ArrayUtils.addAll(encryptedData, encryptedIndex));

        blockLookahead = blockEncStrategyLookahead.decryptBlock(blockEncrypted, secretKey);
        assertThat("Correct data", blockLookahead.getData(), is(blockData));
        assertThat("Correct address", blockLookahead.getAddress(), is(addressInt));
        assertThat("Correct row index", blockLookahead.getRowIndex(), is(rowIndex));
        assertThat("Correct col index", blockLookahead.getColIndex(), is(colIndex));

        BlockLookahead block = new BlockLookahead(23, new byte[]{21, 23, 65, 23, 65, 32, 65, 87, 23, 65, 8, 79, 3, 4, 66, 54, 34, 56, 7, 89, 0, 8, 76, 54, 32});
        block.setIndex(new Index(52, 93));
        BlockEncrypted blockEncryptedSimple = blockEncStrategyLookahead.encryptBlock(block, secretKey);
        BlockLookahead blockDecryptedSimple = blockEncStrategyLookahead.decryptBlock(blockEncryptedSimple, secretKey);
        assertThat(block, is(blockDecryptedSimple));
    }

    @Test
    public void shouldBeAbleToEncryptAListOfBlocks() {
        byte[] bytes0 = Util.getRandomByteArray(14);
        byte[] bytes2 = Util.getRandomByteArray(16);
        BlockLookahead block0 = new BlockLookahead(41, bytes0, 2, 4);
        BlockLookahead block2 = new BlockLookahead(43, bytes2, 4, 6);

//        Define method specific encryption
        EncryptionStrategy encryptionStrategy = new EncryptionStrategyImpl();
        SecretKey secretKey = encryptionStrategy.generateSecretKey(defaultKey);
        factory.setEncryptionStrategy(encryptionStrategy);
        access = new AccessStrategyLookahead(defaultSize, defaultMatrixSize, defaultKey, factory, 0, null, 0);

        BlockEncryptionStrategyLookahead blockEncStrategy = new BlockEncryptionStrategyLookahead(encryptionStrategy);
        List<BlockEncrypted> encryptedBlocks = blockEncStrategy.encryptBlocks(
                Arrays.asList(block0, null, block2), secretKey);
        assertThat(encryptedBlocks, hasSize(3));

//        First block
        byte[] encryptedData = encryptedBlocks.get(0).getData();
        byte[] dataPart = Arrays.copyOf(encryptedData, 32);
        byte[] indexPart = Arrays.copyOfRange(encryptedData, 32, 64);

        byte[] address = encryptionStrategy.decrypt(encryptedBlocks.get(0).getAddress(), secretKey);
        byte[] data = encryptionStrategy.decrypt(dataPart, secretKey);
        byte[] indices = encryptionStrategy.decrypt(indexPart, secretKey);

        assertThat(address, is(Util.leIntToByteArray(41)));
        assertThat(data, is(bytes0));
        assertThat(indices, is(ArrayUtils.addAll(Util.leIntToByteArray(2), Util.leIntToByteArray(4))));

//        Second block
        assertNull(encryptedBlocks.get(1));

//        Third block
        encryptedData = encryptedBlocks.get(2).getData();
        dataPart = Arrays.copyOf(encryptedData, encryptedData.length - 32);
        indexPart = Arrays.copyOfRange(encryptedData, encryptedData.length - 32, encryptedData.length);

        address = encryptionStrategy.decrypt(encryptedBlocks.get(2).getAddress(), secretKey);
        data = encryptionStrategy.decrypt(dataPart, secretKey);
        indices = encryptionStrategy.decrypt(indexPart, secretKey);

        assertThat(address, is(Util.leIntToByteArray(43)));
        assertThat(data, is(bytes2));
        assertThat(indices, is(ArrayUtils.addAll(Util.leIntToByteArray(4), Util.leIntToByteArray(6))));
    }

    private void setStandardServer() {
        CommunicationStrategyStub communicationStrategyStub = new CommunicationStrategyStub(4, 6);
        BlockLookahead[] blocks = new BlockLookahead[24];

//        Column 0
        BlockLookahead block0 = new BlockLookahead(1, Util.leIntToByteArray(0), 0, 0);
        BlockLookahead block1 = new BlockLookahead(2, Util.leIntToByteArray(1), 1, 0);
        BlockLookahead block2 = new BlockLookahead(3, Util.leIntToByteArray(2), 2, 0);
        BlockLookahead block3 = new BlockLookahead(4, Util.leIntToByteArray(3), 3, 0);
//        Column 1
        BlockLookahead block4 = new BlockLookahead(5, Util.leIntToByteArray(4), 0, 1);
        BlockLookahead block5 = new BlockLookahead(6, Util.leIntToByteArray(5), 1, 1);
        BlockLookahead block6 = new BlockLookahead(7, Util.leIntToByteArray(6), 2, 1);
        BlockLookahead block7 = new BlockLookahead(8, Util.leIntToByteArray(7), 3, 1);
//        Column 2
        BlockLookahead block8 = new BlockLookahead(9, Util.leIntToByteArray(8), 0, 2);
        BlockLookahead block9 = new BlockLookahead(10, Util.leIntToByteArray(9), 1, 2);
        BlockLookahead block10 = new BlockLookahead(11, Util.leIntToByteArray(10), 2, 2);
        BlockLookahead block11 = new BlockLookahead(12, Util.leIntToByteArray(11), 3, 2);
//        Column 3
        BlockLookahead block12 = new BlockLookahead(13, Util.leIntToByteArray(12), 0, 3);
        BlockLookahead block13 = new BlockLookahead(14, Util.leIntToByteArray(13), 1, 3);
        BlockLookahead block14 = new BlockLookahead(15, Util.leIntToByteArray(14), 2, 3);
        BlockLookahead block15 = new BlockLookahead(16, Util.leIntToByteArray(15), 3, 3);

//        Access stash
        blocks[16] = block0;
        blocks[17] = block10;
        blocks[18] = block12;
        blocks[19] = block3;
//        Swap stash
        blocks[20] = block14;
        blocks[21] = block7;
        blocks[22] = block9;
        blocks[23] = block8;
//        Matrix
        blocks[1] = block1;
        blocks[2] = block2;
        blocks[5] = block5;
        blocks[4] = block4;
        blocks[6] = block6;
        blocks[11] = block11;
        blocks[13] = block13;
        blocks[15] = block15;

        List<BlockLookahead> blockLookaheads = Arrays.asList(blocks);

        BlockEncryptionStrategyLookahead blockEncStrategy =
                new BlockEncryptionStrategyLookahead(factory.getEncryptionStrategy());
        List<BlockEncrypted> encryptedList = blockEncStrategy.encryptBlocks(
                blockLookaheads, factory.getEncryptionStrategy().generateSecretKey(defaultKey));

        BlockEncrypted[] blocksList = new BlockEncrypted[encryptedList.size()];
        for (int i = 0; i < encryptedList.size(); i++) {
            blocksList[i] = encryptedList.get(i);
        }
        communicationStrategyStub.setBlocks(blocksList);

        access = new AccessStrategyLookahead(defaultSize, defaultMatrixSize, defaultKey,
                new FactoryStub(communicationStrategyStub), 0, null, 0);
    }

    @Test
    public void shouldBeAbleToCreateALookaheadDummyBlock() {
        BlockLookahead block = access.getLookaheadDummyBlock();
        assertNotNull(block);
        assertThat(block.getAddress(), is(0));
        assertThat(block.getData(), is(new byte[Constants.BLOCK_SIZE]));
        assertThat(block.getIndex(), is(new Index(0, 0)));
    }

    @Test
    public void shouldBeAbleToEncryptAndDecryptDummyBlocks() {
        BlockLookahead block = access.getLookaheadDummyBlock();
        BlockEncrypted encryptedBlock = blockEncStrategyLookahead.encryptBlock(block, secretKey);
        BlockLookahead decryptedBlock = blockEncStrategyLookahead.decryptBlock(encryptedBlock, secretKey);
        assertThat(decryptedBlock, is(block));
    }

    @Test
    public void shouldBeAbleToCalculateFlatArrayIndexFromMatrixIndex() {
        assertThat(access.getFlatArrayIndex(new Index(0, 0)), is(0));
        assertThat(access.getFlatArrayIndex(new Index(3, 3)), is(15));
        assertThat(access.getFlatArrayIndex(new Index(0, 1)), is(4));
        assertThat(access.getFlatArrayIndex(new Index(0, 2)), is(8));
        assertThat(access.getFlatArrayIndex(new Index(3, 1)), is(7));
        assertThat(access.getFlatArrayIndex(new Index(2, 2)), is(10));
    }

    @Test
    public void shouldBeAbleToCalculateMatrixIndexFromFlatArrayIndex() {
        assertThat(access.getIndexFromFlatArrayIndex(0), is(new Index(0, 0)));
        assertThat(access.getIndexFromFlatArrayIndex(15), is(new Index(3, 3)));
        assertThat(access.getIndexFromFlatArrayIndex(4), is(new Index(0, 1)));
        assertThat(access.getIndexFromFlatArrayIndex(8), is(new Index(0, 2)));
        assertThat(access.getIndexFromFlatArrayIndex(7), is(new Index(3, 1)));
        assertThat(access.getIndexFromFlatArrayIndex(10), is(new Index(2, 2)));
    }
}
