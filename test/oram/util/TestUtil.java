package oram.util;

import java.util.Arrays;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class TestUtil {
    public static byte[] removeTrailingZeroes(byte[] array) {
        if (array == null) return null;
        int zeros = 0;
        int length = array.length;
        for (int i = length - 1; i >= 0; i--) {
            if (array[i] == 0)
                zeros++;
            else
                break;
        }

        return Arrays.copyOf(array, length - zeros);
    }
}
