package oram;

import oram.block.Block;
import oram.block.BlockEncrypted;
import oram.encryption.EncryptionStrategy;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 20-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class Util {
    public static byte[] getRandomByteArray(int length) {
        if (length <= 0) return new byte[0];

        SecureRandom random = new SecureRandom();
        byte[] res = new byte[length];
        random.nextBytes(res);

        return res;
    }

    public static boolean isDummyBlock(Block block) {
        if (block == null || block.getData() == null || block.getData().length == 0) return false;
        for (byte bit : block.getData())
            if (bit != 0) return false;
        return true;
    }

    public static boolean isDummyAddress(int address) {
        return address == 0;
    }

    //    The following two functions are from
//    The one for big endian integers is an adoption of those
//    https://stackoverflow.com/questions/5399798/byte-array-and-int-conversion-in-java/11419863
//        TODO: Needs testing. All numbers from 0 to like 100.
    public static int byteArrayToLeInt(byte[] b) {
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    //        TODO: Needs testing. All numbers from 0 to like 100.
    public static byte[] leIntToByteArray(int i) {
        final ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(i);
        return bb.array();
    }

    //        TODO: Needs testing. All numbers from 0 to like 100.
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

    //    TODO: Test, and for bytes with negative values
    public static String printByteArray(byte[] array, boolean trimEnd) {
        if (array == null) return null;

        if (trimEnd)
            array = removeTrailingZeroes(array);

        Range<Integer> oneCipher = Range.between(0, 9);
        Range<Integer> twoCiphers = Range.between(10, 99);
        Range<Integer> treeCiphers = Range.between(100, 127);
        Range<Integer> oneCipherNegative = Range.between(-9, -0);
        Range<Integer> twoCiphersNegative = Range.between(-99, -10);

        StringBuilder builder = new StringBuilder("[");
        for (byte b : array) {
            int i = b;
            if (oneCipher.contains(i))
                builder.append("   ");
            else if (twoCiphers.contains(i) || oneCipherNegative.contains(i))
                builder.append("  ");
            else if (treeCiphers.contains(i) || twoCiphersNegative.contains(i))
                builder.append(" ");
            builder.append(b);
            builder.append(",");
        }
        if (builder.length() > 1)
            builder.deleteCharAt(builder.length() - 1);
        return builder.append("]").toString();
    }

    public static byte[] removeTrailingZeroes(byte[] array) {
        if (array == null) return null;
        int i = array.length - 1;
        while (i >= 0 && array[i] == 0)
            --i;

        return Arrays.copyOf(array, i + 1);
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
            int firstIndexInBucket = index * bucketSize;
            int currentIndex = firstIndexInBucket + i;
            if (i == 0)
                builder.append(prefix).append(StringUtils.leftPad(String.valueOf(index), 2)).append(": ");
            else
                builder.append(prefix).append("    ");
            if (array.length > currentIndex)
                builder.append(array[currentIndex].toStringShort());
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

    public static BlockEncrypted getEncryptedDummy(SecretKey key, EncryptionStrategy encryptionStrategy) {
        byte[] encryptedAddress = encryptionStrategy.encrypt(Util.leIntToByteArray(0), key);
        byte[] encryptedData = encryptionStrategy.encrypt(new byte[Constants.BLOCK_SIZE], key);
        return new BlockEncrypted(encryptedAddress, encryptedData);
    }

    public static String getTimeString(long milliseconds) {
        int hours = (int) (milliseconds / 3600000);
        int minutes = (int) (milliseconds % 3600000) / 60000;
        int seconds = (int) (milliseconds % 60000) / 1000;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
