package oram.path;

import oram.Constants;
import oram.OperationType;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockStandard;
import oram.clientcom.CommunicationStrategy;
import oram.clientcom.CommunicationStrategyTiming;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyTiming;
import oram.factory.Factory;
import oram.factory.FactoryCustom;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static oram.factory.FactoryCustom.*;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 18-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class MainPath {
    private static final Logger logger = LogManager.getLogger("log");

    public static void main(String[] args) {
        byte[] key = Constants.KEY_BYTES;

        int numberOfBlocks = 30;
        int bucketSize = 4;
        int size = 31;
        int numberOfRounds = 250;

        BlockStandard[] blockArray = new BlockStandard[(numberOfBlocks + 1)];

        Factory factory = new FactoryCustom(Enc.IMPL, Com.IMPL, Per.IMPL, size, bucketSize);

        CommunicationStrategy communicationStrategy = factory.getCommunicationStrategy();
        communicationStrategy.start();
        AccessStrategyPath access = new AccessStrategyPath(size, bucketSize, key, factory);

        SecureRandom randomness = new SecureRandom();
        List<Integer> addresses = new ArrayList<>();
        for (int i = 0; i < numberOfRounds / 2; i++)
            addresses.add(randomness.nextInt(numberOfBlocks) + 1);

        logger.info("Size: " + size + ", bucket size: " + bucketSize + ", doing rounds: " + numberOfRounds + ", with number of blocks: " + numberOfBlocks);
        System.out.println("Size: " + size + ", bucket size: " + bucketSize + ", doing rounds: " + numberOfRounds + ", with number of blocks: " + numberOfBlocks);

        List<Integer> addressesWrittenTo = new ArrayList<>();
        long startTime = System.nanoTime();
        for (int i = 0; i < numberOfRounds; i++) {
//            printTreeFromServer(size, bucketSize, communicationStrategy, access);
            int address = addresses.get(i % addresses.size());

            boolean writing = i < numberOfRounds / 2;
            OperationType op;
            byte[] data;
            if (writing) {
                op = OperationType.WRITE;
                data = Util.getRandomByteArray(Constants.BLOCK_SIZE);
            } else {
                op = OperationType.READ;
                data = null;
            }

            byte[] res = access.access(op, address, data);

            if (res == null)
                break;

            if (addressesWrittenTo.contains(address)) {
                if (res.length == 0) {
                    break;
                } else {
//                    res = Util.removeTrailingZeroes(res);
                    if (!Arrays.equals(res, blockArray[address].getData())) {
                        System.out.println("SHIT WENT WRONG!!! - WRONG BLOCK!!!");
                        System.out.println("The arrays, that weren't the same:");
                        System.out.println("    res: " + Arrays.toString(res));
                        System.out.println("    old: " + Arrays.toString(blockArray[address].getData()));
                        System.out.println("Block array");
                        for (int j = 0; j < blockArray.length; j++) {
                            String string = blockArray[j] != null ? blockArray[j].toStringShort() : "null";
                            System.out.println("    " + j + ": " + string);
                        }
                        break;
                    }
                }
            } else
                addressesWrittenTo.add(address);

            logger.info("Accessed block " + StringUtils.leftPad(String.valueOf(address), 2) + ", op type: " + op + ", data: " + (data != null ? new String(data) : null) + " in round: " + StringUtils.leftPad(String.valueOf(i), 4));
            System.out.println("Accessed block " + StringUtils.leftPad(String.valueOf(address), 2) + ", op type: " + op + ", in round: " + StringUtils.leftPad(String.valueOf(i), 4));

            if (op.equals(OperationType.WRITE)) blockArray[address] = new BlockStandard(address, data);

            Util.printPercentageDone(startTime, numberOfRounds, i);
        }

//        printTreeFromServer(size, bucketSize, communicationStrategy, access);

        System.out.println("Max stash size: " + access.maxStashSize);
        logger.info("Max stash size: " + access.maxStashSize);
        System.out.println("Max stash size between accesses: " + access.maxStashSizeBetweenAccesses);
        logger.info("Max stash size between accesses: " + access.maxStashSizeBetweenAccesses);

        System.out.println("Overwriting with dummy blocks");
        if (communicationStrategy.sendEndSignal())
            System.out.println("Successfully rewrote all the blocks");
        else
            System.out.println("Unable to overwrite the blocks on the server");

        EncryptionStrategy encryptionStrategy = factory.getEncryptionStrategy();
        if (encryptionStrategy instanceof EncryptionStrategyTiming)
            System.out.println("Encryption time: " +
                    Util.getTimeString(((EncryptionStrategyTiming) encryptionStrategy).getTime() / 1000000));

        if (communicationStrategy instanceof CommunicationStrategyTiming)
            System.out.println("Communication time: " +
                    Util.getTimeString(((CommunicationStrategyTiming) communicationStrategy).getTime() / 1000000));
    }

    private static void printTreeFromServer(int size, int bucketSize, CommunicationStrategy com, AccessStrategyPath access) {
        BlockEncrypted[] array = new BlockEncrypted[size * bucketSize];
        for (int j = 0; j < array.length; j++)
            array[j] = com.read(j);
        System.out.println(Util.printTree(array, bucketSize, access));
    }

    private static String stringOf(String s, int number) {
        if (number < 1) return "";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < number; i++)
            builder.append(s);
        return builder.toString();
    }
}
