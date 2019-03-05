import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 20-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class Util {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static byte[] sizedByteArrayWithInt(int i, int size) {
//        byte[] intArray = new byte[]{new Integer(i).byteValue()};
        int bytes = numberOfBytesForInt(i);
        if (bytes > size) return null;
        byte[] intArray = leIntToByteArray(i);
        byte[] res = new byte[size];
        System.arraycopy(intArray, 0, res, 0, bytes);
        return res;
    }

    public static byte[] getRandomByteArray(int length) {
        if (length <= 0) return new byte[0];

        SecureRandom random = new SecureRandom();
        byte[] res = new byte[length];
        random.nextBytes(res);

        return res;
    }

    public static boolean isDummyAddress(int address) {
        return address == 0;
    }

    //    The following two functions are from
//    https://stackoverflow.com/questions/5399798/byte-array-and-int-conversion-in-java/11419863
    public static int byteArrayToLeInt(byte[] b) {
//        Needs for testing. All numbers from 0 to like 100.
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    public static int byteArrayToBeInt(byte[] b) {
//        Needs for testing. All numbers from 0 to like 100.
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getInt();
    }

    public static byte[] leIntToByteArray(int i) {
        final ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(i);
        return bb.array();
    }

    public static byte[] beIntToByteArray(int i) {
        final ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(i);
        return bb.array();
    }

    public static int numberOfBytesForInt(int i) {
        int count = 0;
        while (Math.abs(i) > 0) {
            count += 1;
            i = i >>> 8;
        }
        return count;
    }

}
