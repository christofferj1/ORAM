package oram.lookahead;

import oram.ServerStub;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 04-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class AccessStrategyLookaheadTest {

    @Test
    public void shouldAddTheBlocksCorrectlyToTheStashMap() {
        int size = 16;
        int matrixSize = 4;
        String key = "Some key 0";
        AccessStrategyLookahead access = new AccessStrategyLookahead(size, matrixSize, key, new ServerStub(0, 0));

//        Adding first block
        BlockLookahead block = new BlockLookahead(17, new byte[]{32});
        int block0Col = 3;
        int block0Row = 4;
        Map<Integer, Map<Integer, BlockLookahead>> value = access.addToAccessStashMap(new HashMap<>(), block0Col, block0Row, block);
        assertThat("Outer map should include col 3", value, hasKey(block0Col));
        assertThat("Inner map should include row 4", value.get(block0Col), hasEntry(block0Row, block));
        assertThat("Outer map has 1 element", value, aMapWithSize(1));
        assertThat("Inner map 3 has 1 element", value.get(block0Col), aMapWithSize(1));

//        Adding block to same column (just extending the inner map)
        int block1Col = 3;
        int block1Row = 1;
        value = access.addToAccessStashMap(value, block1Col, block1Row, block);
        assertThat("Outer map should include col 3", value, hasKey(block0Col));
        assertThat("Inner map should include row 4", value.get(block0Col), hasEntry(block0Row, block));
        assertThat("Inner map should include row 1", value.get(block1Col), hasEntry(block1Row, block));
        assertThat("Outer map has 1 element", value, aMapWithSize(1));
        assertThat("Inner map 3 has 2 elements", value.get(block0Col), aMapWithSize(2));

//        Adding block to a new column
        int block2Col = 1;
        int block2Row = 4;
        value = access.addToAccessStashMap(value, block2Col, block2Row, block);
        assertThat("Outer map should include col 3", value, hasKey(block0Col));
        assertThat("Outer map should include col 1", value, hasKey(block2Col));
        assertThat("Inner map should include row 4", value.get(block0Col), hasEntry(block0Row, block));
        assertThat("Inner map should include row 1", value.get(block1Col), hasEntry(block1Row, block));
        assertThat("Inner map should include row 4", value.get(block2Col), hasEntry(block2Row, block));
        assertThat("Outer map has 2 element", value, aMapWithSize(2));
        assertThat("Inner map 3 has 2 elements", value.get(block0Col), aMapWithSize(2));
        assertThat("Inner map 1 has 1 element", value.get(block2Col), aMapWithSize(1));
    }

}
