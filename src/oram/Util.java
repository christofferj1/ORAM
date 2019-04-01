package oram;

import oram.block.Block;
import oram.block.BlockEncrypted;
import oram.block.BlockLookahead;
import oram.block.BlockStandard;
import oram.encryption.EncryptionStrategy;
import oram.path.AccessStrategyPath;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 20-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class Util {
    private static final Logger logger = LogManager.getLogger("log");
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

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

    public static String printTree(BlockEncrypted[] array, int bucketSize, AccessStrategyPath access) {
        int layers = 0;
        while ((array.length / bucketSize) >= Math.pow(2, layers)) {
            layers++;
        }

        List<BlockEncrypted> encrypted = new ArrayList<>(Arrays.asList(array));

        List<BlockStandard> blockStandards = access.decryptBlockPaths(encrypted, false);
        BlockStandard[] array1 = blockStandards.toArray(new BlockStandard[array.length]);
        return printBucket(array1, bucketSize, 0, 1, layers);
    }

    public static String printBucket(BlockStandard[] array, int bucketSize, int index, int layer, int maxLayers) {
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

    public static String printTreeEncrypted(BlockEncrypted[] array, int bucketSize) {
        int layers = 0;
        while ((array.length / bucketSize) >= Math.pow(2, layers)) {
            layers++;
        }

        return printBucketEncrypted(array, bucketSize, 0, 1, layers);
    }

    public static String printBucketEncrypted(BlockEncrypted[] array, int bucketSize, int index, int layer,
                                              int maxLayers) {
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
            rightChild = printBucketEncrypted(array, bucketSize, 2, layer + 1, maxLayers);
            leftChild = printBucketEncrypted(array, bucketSize, 1, layer + 1, maxLayers);
        } else {
            rightChild = printBucketEncrypted(array, bucketSize, ((index + 1) * 2), layer + 1, maxLayers);
            leftChild = printBucketEncrypted(array, bucketSize, ((index + 1) * 2) - 1, layer + 1, maxLayers);
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
        int millisecondsMod = (int) (milliseconds % 1000);

        String string = String.format("%02d:%02d:%02d.%d", hours, minutes, seconds, millisecondsMod);
        return StringUtils.rightPad(string, 12, '0'); // Adds zeros to the milliseconds
    }

    public static String getMatrixString(BlockLookahead[] blocks, int matrixHeight) {
        StringBuilder builder = new StringBuilder("\n#### Printing matrix and swaps ####\n");
        for (int row = 0; row < matrixHeight; row++) {
            for (int col = 0; col < matrixHeight; col++) {
                int index = col * matrixHeight + row;
                BlockLookahead block = blocks[index];
                if (block != null) {
                    String string = new String(block.getData()).trim();
                    builder.append(StringUtils.rightPad(string.isEmpty() ? "null" : string, 12));
                    builder.append(",");
                    builder.append(StringUtils.leftPad(Integer.toString(block.getAddress()).trim(), 3));
                } else
                    builder.append("       null");
                if (col < matrixHeight - 1)
                    builder.append(" ||| ");
            }
            builder.append("\n");
        }

        builder.append("Swap                         ||| Access\n");
        for (int i = 0; i < matrixHeight; i++) {
            int index = i + matrixHeight * matrixHeight + matrixHeight;
            BlockLookahead block = blocks[index];
            if (block != null) {
                String string = new String(block.getData()).trim();
                builder.append(StringUtils.rightPad(string.isEmpty() ? "null" : string, 12));
                builder.append(", at ").append(StringUtils.leftPad(String.valueOf(block.getAddress()), 2)).append(" ");
                builder.append("(");
                builder.append(StringUtils.leftPad(Integer.toString(block.getRowIndex()).trim(), 2));
                builder.append(", ");
                builder.append(StringUtils.leftPad(Integer.toString(block.getColIndex()).trim(), 2));
                builder.append(")");
            } else
                builder.append("                     null");
            builder.append(" ||| ");
            index -= matrixHeight;
            block = blocks[index];
            if (block != null) {
                String string = new String(block.getData()).trim();
                builder.append(StringUtils.rightPad(string.isEmpty() ? "null" : string, 12));
                builder.append(", at ").append(StringUtils.leftPad(String.valueOf(block.getAddress()), 2)).append(" ");
                builder.append("(");
                builder.append(StringUtils.leftPad(Integer.toString(block.getRowIndex()).trim(), 1));
                builder.append(", ");
                builder.append(StringUtils.leftPad(Integer.toString(block.getColIndex()).trim(), 1));
                builder.append(") ");
            } else
                builder.append("               null");

            builder.append("\n");
        }

        return builder.toString();
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

    public static void printPercentageDone(long startTime, double numberOfRounds, int roundNumber) {
        double percentDone = ((roundNumber + 1) / numberOfRounds) * 100;
        if ((percentDone % 1) == 0) {
            int percentDoneInt = (int) percentDone;
            long timeElapsed = (System.nanoTime() - startTime) / 1000000;
            long timeElapsedPerPercent = timeElapsed / percentDoneInt;
            long timeLeft = timeElapsedPerPercent * (100 - percentDoneInt);
            System.out.println("Done with " + percentDoneInt + "%, time spend: " + getTimeString(timeElapsed) +
                    ", estimated time left: " + getTimeString(timeLeft));
            logger.info("Done with " + percentDoneInt + "%, time spend: " + getTimeString(timeElapsed) +
                    ", estimated time left: " + getTimeString(timeLeft));
        }
    }

    public static String getShortDataString(byte[] data) {
        String dataString;
        if (data.length > 10) {
            String arrayString = printByteArray(Arrays.copyOf(data, 10), false);
            arrayString = arrayString.substring(0, arrayString.length() - 1);
            arrayString += ", ...";
            dataString = arrayString;
        } else
            dataString = printByteArray(data, false);
        return dataString;
    }
}
