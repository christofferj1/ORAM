package oram.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class TestOfTestUtil {
    @Test
    public void shouldBeAbleToRemoveTrailingZeroes() {
        byte[] original = new byte[]{0b01011001};
        byte[] reduced_ = new byte[]{0b01011001};
        assertThat(TestUtil.removeTrailingZeroes(original), is(reduced_));

        original = new byte[]{0b01010000};
        reduced_ = new byte[]{0b01010000};
        assertThat(TestUtil.removeTrailingZeroes(original), is(reduced_));

        original = new byte[]{0b01010000, 0b1010010};
        reduced_ = new byte[]{0b01010000, 0b1010010};
        assertThat(TestUtil.removeTrailingZeroes(original), is(reduced_));

        original = new byte[]{0b01010000, 0b00000000};
        reduced_ = new byte[]{0b01010000};
        assertThat(TestUtil.removeTrailingZeroes(original), is(reduced_));

        original = new byte[]{0b01010000, 0b00000000, 0b01010000, 0b00000000, 0b00000000};
        reduced_ = new byte[]{0b01010000, 0b00000000, 0b01010000};
        assertThat(TestUtil.removeTrailingZeroes(original), is(reduced_));
    }
}
