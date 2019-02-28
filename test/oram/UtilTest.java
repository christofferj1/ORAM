package oram;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 25-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class UtilTest {
    @Test
    public void shouldCreateSizedByteArraysCorrectly() {
//        Some positive numbers
        assertThat("2 bytes, 0", Util.sizedByteArrayWithInt(0, 2), is(new byte[]{0b00000000, 0b00000000}));
        assertThat("1 byte, 77", Util.sizedByteArrayWithInt(77, 1), is(new byte[]{0b01001101}));
        assertThat("2 bytes, 77", Util.sizedByteArrayWithInt(77, 2), is(new byte[]{0b01001101, 0b00000000}));
        assertThat("3 bytes, 2892341", Util.sizedByteArrayWithInt(2892341, 3),
                is(new byte[]{0b00110101, 0b00100010, 0b00101100}));
        assertThat("3 bytes, 2892341", Util.sizedByteArrayWithInt(2892341, 6),
                is(new byte[]{0b00110101, 0b00100010, 0b00101100, 0b00000000, 0b00000000, 0b00000000}));

//        Some negative numbers
        assertNull("-2 bytes, 0", Util.sizedByteArrayWithInt(0, -2));
        assertThat("2 bytes, -0", Util.sizedByteArrayWithInt(-0, 2), is(new byte[]{0b00000000, 0b00000000}));
        assertThat("1 byte, 77", Util.sizedByteArrayWithInt(77, 1), is(new byte[]{0b01001101}));
        assertThat("2 bytes, 77", Util.sizedByteArrayWithInt(77, 2), is(new byte[]{0b01001101, 0b00000000}));
        assertThat("3 bytes, 2892341", Util.sizedByteArrayWithInt(2892341, 3),
                is(new byte[]{0b00110101, 0b00100010, 0b00101100}));
        assertThat("3 bytes, 2892341", Util.sizedByteArrayWithInt(2892341, 6),
                is(new byte[]{0b00110101, 0b00100010, 0b00101100, 0b00000000, 0b00000000, 0b00000000}));
    }
//        Integer.toBinaryString(8233)

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
    public void shouldGiveRandomStringOfRightLength() {
        assertThat(Util.getRandomString(14).length(), is(14));
        assertThat(Util.getRandomString(0).length(), is(0));
        assertThat(Util.getRandomString(-1).length(), is(0));
    }
}
