package oram;

import java.security.SecureRandom;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 20-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class Util {
    static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static byte[] byteFromInt(int i) {
        return new byte[]{new Integer(i).byteValue()};
    }

    public static String getRandomString(int length) {
        if (length <= 0) return "";

        char[] charactersArray = CHARACTERS.toCharArray();
        SecureRandom secureRandom = new SecureRandom();

        char[] res = new char[length];
        for (int i = 0; i < length; i++) {
            res[i] = charactersArray[secureRandom.nextInt(charactersArray.length)];
        }
        return new String(res);
    }

    public static boolean isDummyBlock(byte[] data) {
        for (byte bit : data)
            if (bit != 0) return false;
        return true;
    }
}
