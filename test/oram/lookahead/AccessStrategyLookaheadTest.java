package oram.lookahead;

import oram.*;
import oram.path.BlockStandard;
import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

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
    private int defaultSize;
    private int defaultMatrixSize;

    @Before
    public void setUp() {
        defaultKey = "Some key".getBytes();
        defaultSize = 16;
        defaultMatrixSize = 4;
        access = new AccessStrategyLookahead(defaultSize, defaultMatrixSize, defaultKey, secretKey, new ServerStub(0, 0), encryptionStrategy);
    }

    @Test
    public void shouldBeAbleToConvertStandardBlocksToLookaheadBlocks() {
        BlockStandard block1 = new BlockStandard(11, "Block 1".getBytes());
        BlockStandard block2 = new BlockStandard(2, "Block 2".getBytes());
        BlockStandard block3 = new BlockStandard(8, "Block 3".getBytes());
        BlockStandard block4 = new BlockStandard(15, "Block 4".getBytes());
        BlockStandard block5 = new BlockStandard(4, "Block 5".getBytes());
        BlockStandard block6 = new BlockStandard(6, "Block 6".getBytes());
        BlockStandard block7 = new BlockStandard(13, "Block 7".getBytes());
        BlockStandard block8 = new BlockStandard(3, "Block 8".getBytes());
        BlockStandard block9 = new BlockStandard(9, "Block 9".getBytes());
        BlockStandard block10 = new BlockStandard(10, "Block 10".getBytes());
        BlockStandard block11 = new BlockStandard(1, "Block 11".getBytes());
        BlockStandard block12 = new BlockStandard(12, "Block 12".getBytes());
        BlockStandard block13 = new BlockStandard(7, "Block 13".getBytes());
        BlockStandard block14 = new BlockStandard(14, "Block 14".getBytes());
        BlockStandard block15 = new BlockStandard(5, "Block 15".getBytes());
        BlockStandard block16 = new BlockStandard(16, "Block 16".getBytes());
        List<BlockStandard> blocks = new ArrayList<>(Arrays.asList(block1, block2, block3, block4, block5, block6,
                block7, block8, block9, block10, block11, block12, block13, block14, block15, block16));

        List<BlockLookahead> res = access.standardToLookaheadBlocksForSetup(blocks);
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
        Map<Integer, Map<Integer, BlockLookahead>> map = access.getAccessStash();
//        It should have three entries, where the first is a map with two entries
        assertThat(map, aMapWithSize(3));
        assertThat(map, hasKey(0));
        assertThat(map, hasKey(2));
        assertThat(map, hasKey(3));

        Map<Integer, BlockLookahead> map0 = map.get(0);
        assertThat(map0, aMapWithSize(2));
        assertThat(map0, hasEntry(0, new BlockLookahead(0, Util.leIntToByteArray(0), 0, 0)));
        assertThat(map0, hasEntry(3, new BlockLookahead(3, Util.leIntToByteArray(3), 3, 0)));

        Map<Integer, BlockLookahead> map2 = map.get(2);
        assertThat(map2, aMapWithSize(1));
        assertThat(map2, hasEntry(2, new BlockLookahead(10, Util.leIntToByteArray(10), 2, 2)));

        Map<Integer, BlockLookahead> map3 = map.get(3);
        assertThat(map3, aMapWithSize(1));
        assertThat(map3, hasEntry(0, new BlockLookahead(12, Util.leIntToByteArray(12), 0, 3)));
    }

    @Test
    public void shouldBeAbleToCreateSwatStash() {
        setStandardServer();
        List<BlockLookahead> list = access.getSwapStash();

        assertThat(list, Matchers.<Collection<BlockLookahead>>allOf(
                hasSize(4),
                hasItem(new BlockLookahead(7, Util.leIntToByteArray(7), 3, 1)),
                hasItem(new BlockLookahead(8, Util.leIntToByteArray(8), 0, 2)),
                hasItem(new BlockLookahead(9, Util.leIntToByteArray(9), 1, 2)),
                hasItem(new BlockLookahead(14, Util.leIntToByteArray(14), 2, 3))
        ));
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
    public void shouldBeAbleToReadCorrectIndexInMatrix() {
        setStandardServer();
        BlockLookahead block = access.fetchBlockFromMatrix(new Index(3, 2));
        assertThat(block, equalTo(new BlockLookahead(11, Util.leIntToByteArray(11), 3, 2)));

        block = access.fetchBlockFromMatrix(new Index(1, 0));
        assertThat(block, equalTo(new BlockLookahead(1, Util.leIntToByteArray(1), 1, 0)));

        block = access.fetchBlockFromMatrix(new Index(0, 1));
        assertThat(block, equalTo(new BlockLookahead(4, Util.leIntToByteArray(4), 0, 1)));
    }

    @Test
    public void shouldBeAbleToLookUpInAccessStash() {
        BlockLookahead block0 = new BlockLookahead(0, null);
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
        assertThat(access.findBlockInAccessStash(map, 3, 0), is(block0));
        assertThat(access.findBlockInAccessStash(map, 0, 1), is(block1));
        assertThat(access.findBlockInAccessStash(map, 1, 1), is(block2));
        assertThat(access.findBlockInAccessStash(map, 0, 3), is(block3));
        assertThat(access.findBlockInAccessStash(map, 2, 3), is(block4));

//        All other should return null
        assertNull(access.findBlockInAccessStash(map, 0, 0));
        assertNull(access.findBlockInAccessStash(map, 1, 0));
        assertNull(access.findBlockInAccessStash(map, 2, 0));
        assertNull(access.findBlockInAccessStash(map, 2, 1));
        assertNull(access.findBlockInAccessStash(map, 3, 1));
        assertNull(access.findBlockInAccessStash(map, 0, 2));
        assertNull(access.findBlockInAccessStash(map, 1, 2));
        assertNull(access.findBlockInAccessStash(map, 2, 2));
        assertNull(access.findBlockInAccessStash(map, 3, 2));
        assertNull(access.findBlockInAccessStash(map, 1, 3));
        assertNull(access.findBlockInAccessStash(map, 3, 3));
    }

    @Test
    public void shouldBeAbleToGetLookaheadBlockFromEncryptedBlock() {
        byte[] blockData = Util.getRandomByteArray(10);
        int addressInt = 42;
        int rowIndex = 133742;
        int colIndex = 0;

        byte[] rowBytes = Util.leIntToByteArray(rowIndex);
        byte[] colBytes = Util.leIntToByteArray(colIndex);
        byte[] data = ArrayUtils.addAll(ArrayUtils.addAll(blockData, rowBytes), colBytes);
        byte[] address = Util.leIntToByteArray(addressInt);
        BlockEncrypted blockEncrypted = new BlockEncrypted(AES.encrypt(address, defaultKey),
                AES.encrypt(data, defaultKey));

        BlockLookahead blockLookahead = access.decryptToLookaheadBlock(blockEncrypted);
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
        data = ArrayUtils.addAll(ArrayUtils.addAll(blockData, rowBytes), colBytes);
        address = Util.leIntToByteArray(addressInt);
        blockEncrypted = new BlockEncrypted(AES.encrypt(address, defaultKey),
                AES.encrypt(data, defaultKey));

        blockLookahead = access.decryptToLookaheadBlock(blockEncrypted);
        assertThat("Correct data", blockLookahead.getData(), is(blockData));
        assertThat("Correct address", blockLookahead.getAddress(), is(addressInt));
        assertThat("Correct row index", blockLookahead.getRowIndex(), is(rowIndex));
        assertThat("Correct col index", blockLookahead.getColIndex(), is(colIndex));
    }

    @Test
    public void shouldBeAbleToEncryptAListOfBlocks() {
        byte[] bytes0 = Util.getRandomByteArray(14);
        byte[] bytes2 = Util.getRandomByteArray(16);
        BlockLookahead block0 = new BlockLookahead(41, bytes0, 2, 4);
        BlockLookahead block2 = new BlockLookahead(43, bytes2, 4, 6);

        List<BlockEncrypted> encryptedBlocks = access.encryptBlocks(Arrays.asList(block0, null, block2));
        assertThat(encryptedBlocks, hasSize(3));
        assertThat(AES.decrypt(encryptedBlocks.get(0).getData(), defaultKey),
                equalTo(ArrayUtils.addAll(ArrayUtils.addAll(bytes0, Util.leIntToByteArray(2)),
                        Util.leIntToByteArray(4))));
        assertNull(encryptedBlocks.get(1));
        assertThat(AES.decrypt(encryptedBlocks.get(2).getData(), defaultKey),
                equalTo(ArrayUtils.addAll(ArrayUtils.addAll(bytes2, Util.leIntToByteArray(4)),
                        Util.leIntToByteArray(6))));
    }

    private void setStandardServer() {
        ServerStub server = new ServerStub(4, 6);
        BlockLookahead[] blocks = new BlockLookahead[24];

//        Column 0
        BlockLookahead block0 = new BlockLookahead(0, Util.leIntToByteArray(0), 0, 0);
        BlockLookahead block1 = new BlockLookahead(1, Util.leIntToByteArray(1), 1, 0);
        BlockLookahead block2 = new BlockLookahead(2, Util.leIntToByteArray(2), 2, 0);
        BlockLookahead block3 = new BlockLookahead(3, Util.leIntToByteArray(3), 3, 0);
//        Column 1
        BlockLookahead block4 = new BlockLookahead(4, Util.leIntToByteArray(4), 0, 1);
        BlockLookahead block5 = new BlockLookahead(5, Util.leIntToByteArray(5), 1, 1);
        BlockLookahead block6 = new BlockLookahead(6, Util.leIntToByteArray(6), 2, 1);
        BlockLookahead block7 = new BlockLookahead(7, Util.leIntToByteArray(7), 3, 1);
//        Column 2
        BlockLookahead block8 = new BlockLookahead(8, Util.leIntToByteArray(8), 0, 2);
        BlockLookahead block9 = new BlockLookahead(9, Util.leIntToByteArray(9), 1, 2);
        BlockLookahead block10 = new BlockLookahead(10, Util.leIntToByteArray(10), 2, 2);
        BlockLookahead block11 = new BlockLookahead(11, Util.leIntToByteArray(11), 3, 2);
//        Column 3
        BlockLookahead block12 = new BlockLookahead(12, Util.leIntToByteArray(12), 0, 3);
        BlockLookahead block13 = new BlockLookahead(13, Util.leIntToByteArray(13), 1, 3);
        BlockLookahead block14 = new BlockLookahead(14, Util.leIntToByteArray(14), 2, 3);
        BlockLookahead block15 = new BlockLookahead(15, Util.leIntToByteArray(15), 3, 3);

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
        List<BlockEncrypted> encryptedList = access.encryptBlocks(blockLookaheads);
        BlockEncrypted[] blocksList = new BlockEncrypted[encryptedList.size()];
        for (int i = 0; i < encryptedList.size(); i++) {
            blocksList[i] = encryptedList.get(i);
        }
        server.setBlocks(blocksList);

        access = new AccessStrategyLookahead(defaultSize, defaultMatrixSize, defaultKey, secretKey, server, encryptionStrategy);
    }
}
