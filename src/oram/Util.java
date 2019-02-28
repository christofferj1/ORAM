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
        int bytes = numberOfBytesForInt(i);
        if (bytes > size) return null;
        byte[] intArray = leIntToByteArray(i);
        byte[] res = new byte[size];
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
        if (address == 0) return true;
        return false;
    }

    //    The following two functions are from
//    https://stackoverflow.com/questions/5399798/byte-array-and-int-conversion-in-java/11419863
    public static int byteArrayToLeInt(byte[] b) {
//        Needs for testing. All numbers from 0 to like 100.
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    public static byte[] leIntToByteArray(int i) {
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

    //    TODO: TEST
    public static String printByteArray(byte[] array) {
        StringBuilder builder = new StringBuilder("[");
        for (byte b : array) {
            if (b < 10)
                builder.append("  ");
            else if (b < 100)
                builder.append(" ");
            builder.append(b);
            builder.append(", ");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.deleteCharAt(builder.length() - 1);
        return builder.append("]").toString();
    }

    public static String printTree(BlockEncrypted[] array, int bucketSize) {
        int layers = 0;
        while ((array.length / bucketSize) >= Math.pow(2, layers)) {
            layers++;
        }

        return printBucket(array, bucketSize, 0, 1, layers);
    }

    public static String printBucket(BlockEncrypted[] array, int bucketSize, int index, int layer, int maxLayers) {
        StringBuilder prefix = new StringBuilder();
        for (int i = 1; i < layer; i++) {
            prefix.append("        ");
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bucketSize; i++) {
            int i2 = index * bucketSize;
            int i1 = i2 + i;
            if (array.length > i1)
                builder.append(prefix).append(array[i1]);
            builder.append("\n");
        }

        if (index >= Math.pow(2, maxLayers - 1) - 1)
            return builder.toString();


        String rightChild;
        String leftChild;
        if (index == 0) {
            rightChild = printBucket(array, bucketSize, 2, layer + 1, maxLayers);
            leftChild = printBucket(array, bucketSize, 1, layer + 1, maxLayers);
        } else {
            rightChild = printBucket(array, bucketSize, ((index + 1) * 2), layer + 1, maxLayers);
            leftChild = printBucket(array, bucketSize, ((index + 1) * 2) - 1, layer + 1, maxLayers);
        }

        builder.insert(0, rightChild);
        builder.append(leftChild);

        return builder.toString();
    }
}
