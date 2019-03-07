package oram.permutation;

import oram.BlockEncrypted;
import oram.Util;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class PermutationStrategyTest {
    private PermutationStrategy permutationStrategy;

    @Before
    public void setUp() {
        permutationStrategy = new PermutationStrategyImpl();
    }

    @Test
    public void shouldReturnTheSameBlocks() {
        assertNull(permutationStrategy.permuteBlocks(null));

        List<BlockEncrypted> blocks = new ArrayList<>();
        assertThat("Empty list gives empty list", permutationStrategy.permuteBlocks(blocks), is(blocks));

        BlockEncrypted block0 = new BlockEncrypted(Util.leIntToByteArray(1), Util.leIntToByteArray(2));
        blocks.add(block0);
        assertEquals("Array of 1 element must return the same", permutationStrategy.permuteBlocks(blocks), blocks);

        BlockEncrypted block1 = new BlockEncrypted(Util.leIntToByteArray(2), Util.leIntToByteArray(3));
        BlockEncrypted block2 = new BlockEncrypted(Util.leIntToByteArray(3), Util.leIntToByteArray(4));
        BlockEncrypted block3 = new BlockEncrypted(Util.leIntToByteArray(4), Util.leIntToByteArray(5));
        blocks.addAll(Arrays.asList(block1, block2, block3));
        blocks = permutationStrategy.permuteBlocks(blocks);
        assertThat("block0 is still in the list", blocks, hasItem(block0));
        assertThat("block1 is still in the list", blocks, hasItem(block1));
        assertThat("block2 is still in the list", blocks, hasItem(block2));
        assertThat("block3 is still in the list", blocks, hasItem(block3));
    }
}
