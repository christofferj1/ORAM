package oram.lookahead;

import oram.AES;
import oram.BlockEncrypted;
import oram.ServerStub;
import oram.Util;
import org.apache.commons.lang3.ArrayUtils;
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
        setStandardServer2();

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

        BlockLookahead blockLookahead = access.lookaheadBlockFromEncryptedBlock(blockEncrypted);
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

        blockLookahead = access.lookaheadBlockFromEncryptedBlock(blockEncrypted);
        assertThat("Correct data", blockLookahead.getData(), is(blockData));
        assertThat("Correct address", blockLookahead.getAddress(), is(addressInt));
        assertThat("Correct row index", blockLookahead.getRowIndex(), is(rowIndex));
        assertThat("Correct col index", blockLookahead.getColIndex(), is(colIndex));
    }

    @Test
    public void shouldBeAbleToEncryptAListOfBlocks() {
        byte[] bytes0 = Util.getRandomByteArray(14);
        byte[] bytes1 = Util.getRandomByteArray(15);
        byte[] bytes2 = Util.getRandomByteArray(16);
        BlockLookahead block0 = new BlockLookahead(41, bytes0, 2, 4);
        BlockLookahead block1 = new BlockLookahead(42, bytes1, 3, 5);
        BlockLookahead block2 = new BlockLookahead(43, bytes2, 4, 6);

        List<BlockEncrypted> encryptedBlocks = access.encryptBlocks(Arrays.asList(block0, null, block2));
        assertThat(encryptedBlocks, hasSize(3));
        assertThat(AES.decrypt(encryptedBlocks.get(0).getData(),defaultKey),
                equalTo(ArrayUtils.addAll(ArrayUtils.addAll(bytes0, Util.leIntToByteArray(2)),
                        Util.leIntToByteArray(4))));
        assertNull(encryptedBlocks.get(1));
        assertThat(AES.decrypt(encryptedBlocks.get(2).getData(),defaultKey),
                equalTo(ArrayUtils.addAll(ArrayUtils.addAll(bytes2, Util.leIntToByteArray(4)),
                        Util.leIntToByteArray(6))));
    }

    private void setStandardServer(int matrixWidth) {
        ServerStub server = new ServerStub(matrixWidth, matrixWidth + 2);
        int matrixSize = matrixWidth * matrixWidth;
        BlockLookahead[] blocks = new BlockLookahead[matrixWidth * (matrixWidth + 2)];
//        Add matrix values
        for (int i = 0; i < matrixWidth; i++) {
            for (int j = 0; j < matrixWidth; j++) {
                int address = i + j * 4;
                byte[] data = Util.getRandomByteArray(15);
                blocks[address] = new BlockLookahead(address, data, j, i);
            }
        }
//        Move some blocks to access stash
        List<Integer> movedBlocks = new ArrayList<>();
        for (int i = 0; i < matrixWidth; i++) {
            int index = new Random().nextInt(matrixSize);
            while (movedBlocks.contains(index))
                index = new Random().nextInt(matrixSize);
            blocks[matrixSize + i] = blocks[index];
            blocks[index] = null;
        }
//        Move some blocks to access stash
        for (int i = 0; i < matrixWidth; i++) {
            int index = new Random().nextInt(matrixSize);
            while (movedBlocks.contains(index))
                index = new Random().nextInt(matrixSize);
            blocks[matrixSize + i + 4] = blocks[index];
            blocks[index] = null;
        }

        List<BlockLookahead> blockLookaheads = Arrays.asList(blocks);
        List<BlockEncrypted> encryptedList = access.encryptBlocks(blockLookaheads);
        BlockEncrypted[] blocksList = new BlockEncrypted[encryptedList.size()];
        for (int i = 0; i < encryptedList.size(); i++) {
            blocksList[i] = encryptedList.get(i);
        }
        server.setBlocks(blocksList);

        access = new AccessStrategyLookahead(defaultSize, defaultMatrixSize, defaultKey, server);
    }

    private void setStandardServer2() {
        ServerStub server = new ServerStub(4, 6);
        BlockLookahead[] blocks = new BlockLookahead[24];

        BlockLookahead block0 = new BlockLookahead(0, Util.getRandomByteArray(13), 0, 0);
        BlockLookahead block1 = new BlockLookahead(1, Util.getRandomByteArray(14), 1, 0);
        BlockLookahead block2 = new BlockLookahead(2, Util.getRandomByteArray(15), 2, 0);
        BlockLookahead block3 = new BlockLookahead(3, Util.getRandomByteArray(16), 3, 0);
        BlockLookahead block4 = new BlockLookahead(4, Util.getRandomByteArray(17), 0, 1);
        BlockLookahead block5 = new BlockLookahead(5, Util.getRandomByteArray(18), 1, 1);
        BlockLookahead block6 = new BlockLookahead(6, Util.getRandomByteArray(19), 2, 1);
        BlockLookahead block7 = new BlockLookahead(7, Util.getRandomByteArray(20), 3, 1);
        BlockLookahead block8 = new BlockLookahead(8, Util.getRandomByteArray(21), 0, 2);
        BlockLookahead block9 = new BlockLookahead(9, Util.getRandomByteArray(22), 1, 2);
        BlockLookahead block10 = new BlockLookahead(10, Util.getRandomByteArray(23), 2, 2);
        BlockLookahead block11 = new BlockLookahead(11, Util.getRandomByteArray(24), 3, 2);
        BlockLookahead block12 = new BlockLookahead(12, Util.getRandomByteArray(25), 0, 3);
        BlockLookahead block13 = new BlockLookahead(13, Util.getRandomByteArray(26), 1, 3);
        BlockLookahead block14 = new BlockLookahead(14, Util.getRandomByteArray(27), 2, 3);
        BlockLookahead block15 = new BlockLookahead(15, Util.getRandomByteArray(28), 3, 3);

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
        blocks[0] = block1;
        blocks[1] = block11;
        blocks[2] = block2;
        blocks[4] = block13;
        blocks[6] = block6;
        blocks[8] = block4;
        blocks[10] = block5;
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
