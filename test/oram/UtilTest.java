package oram;

import oram.path.BlockPath;
import org.junit.Test;

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

        BlockPath block = new BlockPath();
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
        assertNull(Util.printByteArray(array));

        array = new byte[]{0, 1, 10, 100};
        assertThat(Util.printByteArray(array), is("[   0,   1,  10, 100]"));

        array = new byte[]{0, 31, 127, -0, -1, -10, -128};
        assertThat(Util.printByteArray(array), is("[   0,  31, 127,   0,  -1, -10,-128]"));
    }

    @Test
    public void shouldBeAbleToPrintATreeCorrectly() {
        BlockEncrypted block = new BlockEncrypted(new byte[]{0b0, 0b0, 0b0}, new byte[]{0b0, 0b0, 0b0});
        BlockEncrypted[] blocks = new BlockEncrypted[]{block, block,};
        String string =
                "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n";
        assertThat("A single block, bucket size 2", Util.printTree(blocks, 2), is(string));

        blocks = new BlockEncrypted[]{block, block, block, block, block, block, block, block, block, block, block,
                block, block, block};
        string =
                "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n";
        assertThat("3 filled layers", Util.printTree(blocks, 2), is(string));

        blocks = new BlockEncrypted[]{block, block, block, block, block, block, block, block, block, block, block,
                block};
        string =
                "\n" +
                        "\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n";
        assertThat("The number of nodes is not an exponent of two", Util.printTree(blocks, 2), is(string));

        blocks = new BlockEncrypted[]{block, block, block, block, block, block, block, block};
        string =
                "\n" +
                        "\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "\n" +
                        "\n" +
                        "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "\n" +
                        "\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n";
        assertThat("The number of nodes is not an exponent of two", Util.printTree(blocks, 2), is(string));

        blocks = new BlockEncrypted[]{block, block, block, block, block, block, block, block, block, block, block,
                block, block, block, block, block, block, block, block, block, block};
        string =
                "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n";
        assertThat("Trying bucket size 3", Util.printTree(blocks, 3), is(string));
    }

    @Test
    public void shouldPrintBucketsCorrectly() {
        BlockEncrypted block = new BlockEncrypted(new byte[]{0b0, 0b0, 0b0}, new byte[]{0b0, 0b0, 0b0});
        BlockEncrypted[] blocks = new BlockEncrypted[]{block, block, block, block, block, block};
        String string =
                "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n";
        assertThat(Util.printBucket(blocks, 2, 0, 1, 2), is(string));

        blocks = new BlockEncrypted[]{block, block, block, block, block, block, block, block, block, block, block,
                block, block, block};
        string =
                "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "        BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n" +
                        "                BlockEncrypted{address=[   0,   0,   0], data=[   0,   0,   0]}\n";
        assertThat(Util.printBucket(blocks, 2, 0, 1, 3), is(string));
    }
}
