package oram.lookahead;

import oram.AES;
import oram.BlockEncrypted;
import oram.ServerStub;
import oram.Util;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 04-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class AccessStrategyLookaheadTest {
    private AccessStrategyLookahead defaultAccessStrategyLookahead;
    private byte[] defaultKey;
    private int defaultSize;
    private int defaultMatrixSize;
    private ServerStub defaultServer;

    @Before
    public void setUp() {
        defaultKey = "Some key".getBytes();
        defaultSize = 16;
        defaultMatrixSize = 4;
        defaultServer = new ServerStub(0, 0);
        defaultAccessStrategyLookahead = new AccessStrategyLookahead(defaultSize, defaultMatrixSize, defaultKey,
                defaultServer);
    }

    @Test
    public void shouldAddTheBlocksCorrectlyToTheStashMap() {
//        Adding first block
        BlockLookahead block = new BlockLookahead(17, new byte[]{32});
        block.setRowIndex(4);
        block.setColIndex(3);
        Map<Integer, Map<Integer, BlockLookahead>> value = defaultAccessStrategyLookahead.addToAccessStashMap(new HashMap<>(), block);
        assertThat("Outer map should include col 3", value, hasKey(3));
        assertThat("Inner map should include row 4", value.get(3), hasEntry(4, block));
        assertThat("Outer map has 1 element", value, aMapWithSize(1));
        assertThat("Inner map 3 has 1 element", value.get(3), aMapWithSize(1));

//        Adding block to same column (just extending the inner map)
        block.setRowIndex(1);
        block.setColIndex(3);
        value = defaultAccessStrategyLookahead.addToAccessStashMap(value, block);
        assertThat("Outer map should include col 3", value, hasKey(3));
        assertThat("Inner map should include row 4", value.get(3), hasEntry(4, block));
        assertThat("Inner map should include row 1", value.get(3), hasEntry(1, block));
        assertThat("Outer map has 1 element", value, aMapWithSize(1));
        assertThat("Inner map 3 has 2 elements", value.get(3), aMapWithSize(2));

//        Adding block to a new column
        block.setRowIndex(4);
        block.setColIndex(1);
        value = defaultAccessStrategyLookahead.addToAccessStashMap(value, block);
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

        BlockLookahead blockLookahead = defaultAccessStrategyLookahead.lookaheadBlockFromEncryptedBlock(blockEncrypted);
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

        blockLookahead = defaultAccessStrategyLookahead.lookaheadBlockFromEncryptedBlock(blockEncrypted);
        assertThat("Correct data", blockLookahead.getData(), is(blockData));
        assertThat("Correct address", blockLookahead.getAddress(), is(addressInt));
        assertThat("Correct row index", blockLookahead.getRowIndex(), is(rowIndex));
        assertThat("Correct col index", blockLookahead.getColIndex(), is(colIndex));
    }
}
