package oram.lookahead;

import oram.AES;
import oram.BlockEncrypted;
import oram.ServerStub;
import oram.Util;
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
        access = new AccessStrategyLookahead(defaultSize, defaultMatrixSize, defaultKey, new ServerStub(0, 0));
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

        access = new AccessStrategyLookahead(defaultSize, defaultMatrixSize, defaultKey, server);
    }
}
