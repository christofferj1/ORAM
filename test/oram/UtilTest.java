package oram;

import oram.block.BlockEncrypted;
import oram.block.BlockTrivial;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 25-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class UtilTest {
    @Test
    public void shouldReturnTrueIffAllBytesOfAnArrayIsZero() {
        assertFalse(Util.isDummyBlock(null));

        BlockTrivial block = new BlockTrivial();
        assertFalse(Util.isDummyBlock(block));

        block.setData(new byte[0]);
        assertFalse(Util.isDummyBlock(block));

        block.setData(new byte[]{0b0});
        assertTrue(Util.isDummyBlock(block));

        block.setAddress(5);
        assertTrue(Util.isDummyBlock(block));

        block.setData(new byte[]{0b0, 0b00000000, 0b0000000000});
        assertTrue(Util.isDummyBlock(block));

        block.setData(new byte[]{0b00001000});
        assertFalse(Util.isDummyBlock(block));

        block.setData(new byte[]{0b0, 0b00001000, 0b0});
        assertFalse(Util.isDummyBlock(block));
    }

    @Test
    public void shouldReturnTrueIffAddressIsZero() {
        assertFalse(Util.isDummyAddress(-32));
        assertFalse(Util.isDummyAddress(-1));
        assertTrue(Util.isDummyAddress(0));
        assertFalse(Util.isDummyAddress(1));
        assertFalse(Util.isDummyAddress(32));
    }

    @Test
    public void shouldGiveTheRightIntFromByteArray() {
        assertThat("number 0", Util.byteArrayToLeInt(new byte[]{0b00000000, 0b00000000, 0b00000000, 0b00000000}),
                is(0));
        assertThat("number 77", Util.byteArrayToLeInt(new byte[]{0b01001101, 0b00000000, 0b00000000, 0b00000000}),
                is(77));
        assertThat("number 2892341", Util.byteArrayToLeInt(new byte[]{0b00110101, 0b00100010, 0b00101100, 0b00000000}),
                is(2892341));
//        [<-83>, <50>, <61>, <17>]
        assertThat("number 289223341", Util.byteArrayToLeInt(
                new byte[]{0b11111111111111111111111110101101, 0b00110010, 0b00111101, 0b00010001}), is(289223341));
    }

    @Test
    public void shouldGiveCorrectArrayFromInt() {
        assertThat("number 0", Util.leIntToByteArray(0),
                is(new byte[]{0b00000000, 0b00000000, 0b00000000, 0b00000000}));
        assertThat("number 77", Util.leIntToByteArray(77),
                is(new byte[]{0b01001101, 0b00000000, 0b00000000, 0b00000000}));
        assertThat("number 2892341", Util.leIntToByteArray(2892341),
                is(new byte[]{0b00110101, 0b00100010, 0b00101100, 0b00000000}));
//        [<-83>, <50>, <61>, <17>]
        assertThat("number 289223341", Util.leIntToByteArray(289223341),
                is(new byte[]{0b11111111111111111111111110101101, 0b00110010, 0b00111101, 0b00010001}));
    }

    @Test
    public void shouldGiveTheRightNumberOfBytesForInt() {
        assertThat("Number 0", Util.numberOfBytesForInt(0), is(0));
        assertThat("Number 83", Util.numberOfBytesForInt(83), is(1));
        assertThat("Number 830", Util.numberOfBytesForInt(830), is(2));
        assertThat("Number 8300", Util.numberOfBytesForInt(8300), is(2));
        assertThat("Number 65535", Util.numberOfBytesForInt(65535), is(2));
        assertThat("Number 65536", Util.numberOfBytesForInt(65536), is(3));

        assertThat("Number -0", Util.numberOfBytesForInt(-0), is(0));
        assertThat("Number -83", Util.numberOfBytesForInt(-83), is(4));
        assertThat("Number -830", Util.numberOfBytesForInt(-830), is(4));
        assertThat("Number -8300", Util.numberOfBytesForInt(-8300), is(4));
        assertThat("Number -65535", Util.numberOfBytesForInt(-65535), is(4));
        assertThat("Number -65536", Util.numberOfBytesForInt(-65536), is(4));
    }

    @Test
    public void shouldPrettyPrintByteArrays() {
        byte[] array = null;
        assertNull(Util.printByteArray(array, false));

        array = new byte[]{0, 1, 10, 100};
        assertThat(Util.printByteArray(array, false), is("[   0,   1,  10, 100]"));

        array = new byte[]{0, 31, 127, -0, -1, -10, -128};
        assertThat(Util.printByteArray(array, false), is("[   0,  31, 127,   0,  -1, -10,-128]"));
    }

    @Ignore
    @Test
    public void shouldBeAbleToPrintATreeCorrectly() {
        BlockEncrypted block = new BlockEncrypted(new byte[]{0b0, 0b0, 0b0}, new byte[]{0b0, 0b0, 0b0});
        BlockEncrypted[] blocks = new BlockEncrypted[]{block, block,};
        String string =
                " 0: Block{add=[], data=[   0,   0,   0]}\n" +
                        "    Block{add=[], data=[   0,   0,   0]}\n";
        assertThat("A single block, bucket size 2", Util.printTreeEncrypted(blocks, 2), is(string));

        blocks = new BlockEncrypted[]{block, block, block, block, block, block, block, block, block, block, block,
                block, block, block};
        string =
                "                 6: Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "         2: Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "                 5: Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        " 0: Block{add=[], data=[   0,   0,   0]}\n" +
                        "    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                 4: Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "         1: Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "                 3: Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n";
        assertThat("3 filled layers", Util.printTreeEncrypted(blocks, 2), is(string));

        blocks = new BlockEncrypted[]{block, block, block, block, block, block, block, block, block, block, block,
                block};
        string =
                "                 6: \n" +
                        "\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "    Block{add=[], data=[   0,   0,   0]}\n" +
                        "    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n";
        assertThat("The number of nodes is not an exponent of two", Util.printTreeEncrypted(blocks, 2), is(string));

        blocks = new BlockEncrypted[]{block, block, block, block, block, block, block, block};
        string =
                "\n" +
                        "\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "\n" +
                        "\n" +
                        "    Block{add=[], data=[   0,   0,   0]}\n" +
                        "    Block{add=[], data=[   0,   0,   0]}\n" +
                        "\n" +
                        "\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n";
        assertThat("The number of nodes is not an exponent of two", Util.printTreeEncrypted(blocks, 2), is(string));

        blocks = new BlockEncrypted[]{block, block, block, block, block, block, block, block, block, block, block,
                block, block, block, block, block, block, block, block, block, block};
        string =
                "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "    Block{add=[], data=[   0,   0,   0]}\n" +
                        "    Block{add=[], data=[   0,   0,   0]}\n" +
                        "    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n";
        assertThat("Trying bucket size 3", Util.printTreeEncrypted(blocks, 3), is(string));
    }

    @Test
    public void shouldPrintBucketsCorrectly() {
        BlockEncrypted block = new BlockEncrypted(new byte[]{0b0, 0b0, 0b0}, new byte[]{0b0, 0b0, 0b0});
        BlockEncrypted[] blocks = new BlockEncrypted[]{block, block, block, block, block, block};
        String string =
                "         2: Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        " 0: Block{add=[], data=[   0,   0,   0]}\n" +
                        "    Block{add=[], data=[   0,   0,   0]}\n" +
                        "         1: Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n";
        assertThat(Util.printBucketEncrypted(blocks, 2, 0, 1, 2), is(string));

        blocks = new BlockEncrypted[]{block, block, block, block, block, block, block, block, block, block, block,
                block, block, block};
        string =
                "                 6: Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "         2: Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "                 5: Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        " 0: Block{add=[], data=[   0,   0,   0]}\n" +
                        "    Block{add=[], data=[   0,   0,   0]}\n" +
                        "                 4: Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n" +
                        "         1: Block{add=[], data=[   0,   0,   0]}\n" +
                        "            Block{add=[], data=[   0,   0,   0]}\n" +
                        "                 3: Block{add=[], data=[   0,   0,   0]}\n" +
                        "                    Block{add=[], data=[   0,   0,   0]}\n";
        assertThat(Util.printBucketEncrypted(blocks, 2, 0, 1, 3), is(string));
    }

    @Test
    public void shouldBeAbleToCreateDummyMap() {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 1; i <= 17; i++)
            map.put(i, -42);

        Map<Integer, Integer> tmp;
        for (int i = 1; i <= 17; i++) {
            tmp = Util.getDummyMap(i);
            assertThat("Map including: " + i, tmp, is(map));
        }

        map = new HashMap<>();
        int newStart = 17 * 4;
        for (int i = 1; i <= 17; i++) {
            map.put(i + newStart, Constants.DUMMY_LEAF_NODE_INDEX);
        }

        for (int i = newStart + 1; i <= newStart + 17; i++) {
                        tmp = Util.getDummyMap(i);
            assertThat("Map including: " + i, tmp, is(map));
        }

        tmp = Util.getDummyMap(0);
        assertNotNull("Map is not null", tmp);
        for (int i = -16; i <= 0; i++ ) {
            assertTrue("Maps contains: " + i, tmp.containsKey(i));
            assertThat("Maps " + i + " to -42", tmp.get(i), is(Constants.DUMMY_LEAF_NODE_INDEX));
        }
    }

    @Test
    public void shouldBeAbleToComputeLevelSize() {
        assertThat(Util.getLevelSize(0, 4), is(Constants.SIZE_5));
        assertThat(Util.getLevelSize(1, 4), is(Constants.SIZE_4));
        assertThat(Util.getLevelSize(2, 4), is(Constants.SIZE_3));
        assertThat(Util.getLevelSize(3, 4), is(Constants.SIZE_2));
        assertThat(Util.getLevelSize(4, 4), is(Constants.SIZE_1));

        assertThat(Util.getLevelSize(0, 3), is(Constants.SIZE_4));
        assertThat(Util.getLevelSize(1, 3), is(Constants.SIZE_3));
        assertThat(Util.getLevelSize(2, 3), is(Constants.SIZE_2));
        assertThat(Util.getLevelSize(3, 3), is(Constants.SIZE_1));

        assertThat(Util.getLevelSize(0, 2), is(Constants.SIZE_3));
        assertThat(Util.getLevelSize(1, 2), is(Constants.SIZE_2));
        assertThat(Util.getLevelSize(2, 2), is(Constants.SIZE_1));

        assertThat(Util.getLevelSize(0, 1), is(Constants.SIZE_2));
        assertThat(Util.getLevelSize(1, 1), is(Constants.SIZE_1));

        assertThat(Util.getLevelSize(0, 0), is(Constants.SIZE_1));
    }

    @Test
    public void shouldBeAbleToConvertBetweenByteArraysAndMaps() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1,2);
        map.put(0,0);
        map.put(-42, 321432432);
        byte[] bytes = Util.getByteArrayFromMap(map);
        assertThat(Util.getMapFromByteArray(bytes), is(map));
    }
}
