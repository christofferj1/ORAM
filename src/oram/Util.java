package oram;

import oram.block.BlockEncrypted;
import oram.block.BlockPath;
import oram.factory.Factory;
import oram.lookahead.AccessStrategyDummy;
import oram.ofactory.ORAMFactory;
import oram.ofactory.ORAMFactoryLookaheadTrivial;
import oram.path.AccessStrategyPath;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.*;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 20-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class Util {
    private static final Logger logger = LogManager.getLogger("log");

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

    public static int byteArrayToLeInt(byte[] b) {
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

    public static byte[] beIntToByteArray(int i) {
        final ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(i);
        return bb.array();
    }

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
            if (oneCipher.contains((int) b))
                builder.append("   ");
            else if (twoCiphers.contains((int) b) || oneCipherNegative.contains((int) b))
                builder.append("  ");
            else if (treeCiphers.contains((int) b) || twoCiphersNegative.contains((int) b))
                builder.append(" ");
            builder.append(b);
            builder.append(",");
        }
        if (builder.length() > 1)
            builder.deleteCharAt(builder.length() - 1);
        return builder.append("]").toString();
    }

    private static byte[] removeTrailingZeroes(byte[] array) {
        if (array == null) return null;
        int i = array.length - 1;
        while (i >= 0 && array[i] == 0)
            --i;

        return Arrays.copyOf(array, i + 1);
    }

    static String printTree(BlockEncrypted[] array, int bucketSize, AccessStrategyPath access,
                            String prefixString) {
        int layers = 0;
        while ((array.length / bucketSize) >= Math.pow(2, layers)) {
            layers++;
        }

        List<BlockEncrypted> encrypted = new ArrayList<>(Arrays.asList(array));

        List<BlockPath> blockPaths = access.decryptBlockPaths(encrypted, false);
        BlockPath[] array1 = blockPaths.toArray(new BlockPath[array.length]);
        return printBucket(array1, bucketSize, 0, 1, layers, prefixString);
    }

    private static String printBucket(BlockPath[] array, int bucketSize, int index, int layer, int maxLayers,
                                      String prefixString) {
        StringBuilder prefix = new StringBuilder();

        for (int i = 1; i < layer; i++)
            prefix.append("        ");

        StringBuilder builder = new StringBuilder();
        double sizeMinusOne = Math.pow(2, maxLayers - 1) - 1;
        if (index == sizeMinusOne + 1)
            builder.append(prefixString);

        for (int i = 0; i < bucketSize; i++) {
            int firstIndexInBucket = index * bucketSize;
            int currentIndex = firstIndexInBucket + i;
            if (i == 0)
                builder.append(prefix).append(StringUtils.leftPad(String.valueOf(index), 2)).append(": ");
            else
                builder.append(prefix).append("    ");

            if (array.length > currentIndex)
                builder.append(array[currentIndex].toStringShort());

            builder.append("\n").append(prefixString);
        }

        if (index >= sizeMinusOne)
            return builder.toString();


        String rightChild;
        String leftChild;
        if (index == 0) {
            rightChild = printBucket(array, bucketSize, 2, layer + 1, maxLayers, prefixString);
            leftChild = printBucket(array, bucketSize, 1, layer + 1, maxLayers, prefixString);
        } else {
            rightChild = printBucket(array, bucketSize, ((index + 1) * 2), layer + 1, maxLayers, prefixString);
            leftChild = printBucket(array, bucketSize, ((index + 1) * 2) - 1, layer + 1, maxLayers, prefixString);
        }

        builder.insert(0, rightChild);
        builder.append(leftChild);

        return builder.toString();
    }

    static String getTimeString(long milliseconds) {
        int hours = (int) (milliseconds / 3600000);
        int minutes = (int) (milliseconds % 3600000) / 60000;
        int seconds = (int) (milliseconds % 60000) / 1000;
        int millisecondsMod = (int) (milliseconds % 1000);

        String string = String.format("%02d:%02d:%02d.%d", hours, minutes, seconds, millisecondsMod);
        string = StringUtils.rightPad(string, 12, '0');
        return string; // Adds zeros to the milliseconds
    }

    static String getPercentageDoneString(long startTime, double numberOfRounds, int roundNumber) {
        double percentDone = ((roundNumber + 1) / numberOfRounds) * 100;
        percentDone = percentDone * 1000000;
        percentDone = Math.round(percentDone);
        percentDone = percentDone / 1000000;
        if ((percentDone % 1) == 0) {
            int percentDoneInt = (int) percentDone;
            long timeElapsed = (System.nanoTime() - startTime) / 1000000;
            long timeElapsedPerPercent = timeElapsed / percentDoneInt;
            long timeLeft = timeElapsedPerPercent * (100 - percentDoneInt);

            String percent = percentDoneInt < 10 ? " " + percentDoneInt : String.valueOf(percentDoneInt);
            percent = percentDoneInt < 100 ? " " + percent : percent;
            return "Done with " + percent + "%, time spend: " + getTimeString(timeElapsed) +
                    ", estimated time left: " + getTimeString(timeLeft) + ", done: " + getClockString(timeLeft) +
                    " (estimated total: " + getTimeString(timeElapsed + timeLeft) + ")";
        }
        return null;
    }

    private static String getClockString(long timeLeft) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(System.currentTimeMillis() + timeLeft);
        String done = (now.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + now.get(Calendar.HOUR_OF_DAY) : "" +
                now.get(Calendar.HOUR_OF_DAY)) + ":";
        done += (now.get(Calendar.MINUTE) < 10 ? "0" + now.get(Calendar.MINUTE) : now.get(Calendar.MINUTE)) + ":";
        done += (now.get(Calendar.SECOND) < 10 ? "0" + now.get(Calendar.SECOND) : now.get(Calendar.SECOND));
        return done;
    }

    public static String getShortDataString(byte[] data) {
        if (data == null) return null;
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

    public static void logAndPrint(Logger logger, String string) {
        System.out.println(string);
        logger.info(string);
    }

    public static int getInteger(String name) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter integer for '" + name + "'");
        String answer = scanner.nextLine();
        while (!answer.matches("\\d+")) {
            System.out.println("Enter integer for '" + name + "'");
            answer = scanner.nextLine();
        }
        return Integer.parseInt(answer);
    }

    public static Map<Integer, Integer> getMapFromByteArray(byte[] array) {
        if (array == null) return null;
        if (Arrays.equals(array, new byte[0])) return new HashMap<>();
        Map<Integer, Integer> res = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(array);
        try (ObjectInputStream ois = new ObjectInputStream(bis)) {
            res = (Map<Integer, Integer>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Unable to get position map from byte array: " + Arrays.toString(array));
            logger.error(e);
            logger.debug("Stacktrace:", e);
        }
        return res;
    }

    public static Map<Integer, Integer> getDummyMap(int addressToInclude) {
        Integer startAddress;
        if (addressToInclude == 0) {
            startAddress = -16;
        } else {
            startAddress = null;
            for (int i = 0; i < Constants.POSITION_BLOCK_SIZE; i++) {
                int modBlockSize = (addressToInclude - i) % Constants.POSITION_BLOCK_SIZE;
                if (modBlockSize == 1) {
                    startAddress = addressToInclude - i;
                    break;
                }
            }
        }

        if (startAddress == null) {
            logger.error("Unable to find start address for: " + addressToInclude);
            return null;
        }

        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < Constants.POSITION_BLOCK_SIZE; i++)
            map.put(startAddress + i, Constants.DUMMY_LEAF_NODE_INDEX);

        return map;
    }

    public static byte[] getByteArrayFromMap(Map<Integer, Integer> map) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(map);
            oos.flush();
        } catch (IOException e) {
            logger.error("Unable to create byte array for dummy position array");
            logger.error(e);
            logger.debug("Stacktrace:", e);
            return null;
        }
        return bos.toByteArray();
    }

    public static String getEmptyStringOfLength(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++)
            builder.append(" ");
        return builder.toString();
    }

    static String chooseORAMType(String string) {
        Scanner scanner = new Scanner(System.in);
        Util.logAndPrint(logger, string);
        String answer = scanner.nextLine();
        while (!(answer.equals("l") || answer.equals("p") || answer.equals("t") || answer.equals("lt"))) {
            System.out.println("Choose ORAM between Lookahead, Path, Trivial, or Lookahead (using Trivial specialised for Lookahead) [l/lt/p/t]");
            answer = scanner.nextLine();
        }
        logger.info(answer);
        return answer;
    }

    static int getLevelSize(int level, int numberOfORAM) {
        return (int) Math.pow(2, ((numberOfORAM - level) * 4) + 6);
    }

    public static Map<Integer, Integer> getPositionMap(int address, int newPosition, AccessStrategy access) {
        byte[] newPositionBytes = leIntToByteArray(newPosition);
        byte[] positionMapBytes = access.access(OperationType.WRITE, address, newPositionBytes, true,
                false);

        return getMapFromByteArray(positionMapBytes);
    }

    public static List<String> getAddressStrings(int from, int to) {
        List<String> addresses = new ArrayList<>();
        for (int i = from; i < to; i++)
            addresses.add(String.valueOf(i));
        return addresses;
    }

    static List<AccessStrategy> getAccessStrategies(List<ORAMFactory> factories, byte[] key, Factory factory) {
        AccessStrategy[] res = new AccessStrategy[factories.size()];
        for (int i = factories.size() - 1; i >= 0; i--) {
            ORAMFactory oramFactory = factories.get(i);
            int prefixSize = i * 10;

            if (oramFactory instanceof ORAMFactoryLookaheadTrivial)
                res[i] = oramFactory.getAccessStrategy(key, factory, new AccessStrategyDummy(), prefixSize);
            else if (i == factories.size() - 1)
                res[i] = oramFactory.getAccessStrategy(key, factory, null, prefixSize);
            else
                res[i] = oramFactory.getAccessStrategy(key, factory, res[i + 1], prefixSize);
        }
        return new ArrayList<>(Arrays.asList(res));
    }
}
