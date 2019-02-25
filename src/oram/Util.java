package oram;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    public static byte[] sizedByteArrayWithInt(int i, int size) {
//        byte[] intArray = new byte[]{new Integer(i).byteValue()};
        byte[] intArray = leIntToByteArray(i);
        byte[] res = new byte[size];
        int bytes = numberOfBytesForInt(i);
        if (bytes > size) return null;
        System.arraycopy(intArray, 0, res, 0, bytes);
        return res;
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

    public static boolean isDummyBlock(Block block) {
        for (byte bit : block.getData())
            if (bit != 0) return false;
        return true;
    }

    public static boolean isDummyAddress(int address) {
        if (address == 0) return false;
        return true;
    }

//    The following two functions are from
//    https://stackoverflow.com/questions/5399798/byte-array-and-int-conversion-in-java/11419863
    public static int byteArrayToLeInt(byte[] b) {
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    static byte[] leIntToByteArray(int i) {
        final ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(i);
        return bb.array();
    }

    static int numberOfBytesForInt(int i) {
        int count = 0;
        while (Math.abs(i) > 0) {
            count += 1;
            i = i >>> 8;
        }
        return count;
    }
}
