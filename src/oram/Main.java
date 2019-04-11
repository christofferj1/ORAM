package oram;

import oram.block.BlockEncrypted;
import oram.block.BlockStandard;
import oram.clientcom.CommunicationStrategy;
import oram.clientcom.CommunicationStrategyTiming;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyTiming;
import oram.factory.Factory;
import oram.factory.FactoryCustom;
import oram.ofactory.ORAMFactory;
import oram.ofactory.ORAMFactoryLookahead;
import oram.ofactory.ORAMFactoryPath;
import oram.ofactory.ORAMFactoryTrivial;
import oram.path.AccessStrategyPath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static oram.factory.FactoryCustom.*;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class Main {
    private static final Logger logger = LogManager.getLogger("log");

    public static void main(String[] args) {
        byte[] key = Constants.KEY_BYTES;

        ORAMFactory oramFactory = getOramFactory("main");
        Factory factory = new FactoryCustom(Enc.IMPL, Com.IMPL, Per.IMPL, oramFactory.factorySizeParameter0(),
                oramFactory.factorySizeParameter1());

        int numberOfBlocks = oramFactory.getNumberOfBlocks();

        BlockStandard[] blockArray = new BlockStandard[(numberOfBlocks + 1)];
        List<BlockStandard> blocks = new ArrayList<>();
        for (int i = 1; i <= numberOfBlocks; i++) {
            BlockStandard block = new BlockStandard(i, Util.getRandomByteArray(Constants.BLOCK_SIZE));
            blocks.add(block);
            blockArray[i] = block;
        }

        boolean pathORAMChosen = oramFactory instanceof ORAMFactoryPath;
        CommunicationStrategy communicationStrategy = factory.getCommunicationStrategy();
        communicationStrategy.start();

//        Create recursive ORAM
        ORAMFactory oramFactory1 = getOramFactory("1");
        AccessStrategy access1 = oramFactory1.getAccessStrategy(key, factory, null);

        AccessStrategy access = oramFactory.getAccessStrategy(key, factory, access1);
        if (oramFactory instanceof ORAMFactoryLookahead)
            access.setup(blocks);

        SecureRandom randomness = new SecureRandom();
        List<Integer> addresses = new ArrayList<>();
        int numberOfRounds = oramFactory.getNumberOfRounds();
        for (int i = 0; i < numberOfRounds / 2; i++)
            addresses.add(randomness.nextInt(numberOfBlocks) + 1);

        StringBuilder resume = new StringBuilder(oramFactory.getInitString());
        Util.logAndPrint(logger, resume.toString());

        printTreeFromServer(oramFactory.getSize(), oramFactory.getBucketSize(), communicationStrategy, (AccessStrategyPath) access, oramFactory.getOffSet());
        printTreeFromServer(oramFactory1.getSize(), oramFactory1.getBucketSize(), communicationStrategy, (AccessStrategyPath) access1, oramFactory1.getOffSet());

        List<Integer> addressesWrittenTo = new ArrayList<>();
        long startTime = System.nanoTime();
        for (int i = 0; i < numberOfRounds; i++) {
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

            byte[] res = access.access(op, address, data, false);
            if (res == null) break;

//            res = Util.removeTrailingZeroes(res);

            if (addressesWrittenTo.contains(address)) {
                if (pathORAMChosen && res.length == 0)
                    break;
                else {
                    if (!Arrays.equals(res, blockArray[address].getData())) {
                        Util.logAndPrint(logger, "SHIT WENT WRONG!!! - WRONG BLOCK!!!");
                        Util.logAndPrint(logger, "    Address: " + address + ", in: " + Arrays.toString(addressesWrittenTo.toArray()));
                        Util.logAndPrint(logger, "    The arrays, that weren't the same:");
                        Util.logAndPrint(logger, "        res: " + Arrays.toString(res));
                        Util.logAndPrint(logger, "        old: " + Arrays.toString(blockArray[address].getData()));
                        break;
                    }
                }
            } else
                addressesWrittenTo.add(address);

            if (op.equals(OperationType.WRITE)) blockArray[address] = new BlockStandard(address, data);

            System.out.println("Block array");
            for (int j = 0; j < blockArray.length; j++) {
                BlockStandard b = blockArray[j];
                System.out.println("  " + j + ": " + (b != null ? b.toStringShort() : ""));
            }
            System.out.println(" ");
            printTreeFromServer(oramFactory.getSize(), oramFactory.getBucketSize(), communicationStrategy, (AccessStrategyPath) access, oramFactory.getOffSet());
            printTreeFromServer(oramFactory1.getSize(), oramFactory1.getBucketSize(), communicationStrategy, (AccessStrategyPath) access1, oramFactory1.getOffSet());

            String string = Util.getPercentageDoneString(startTime, numberOfRounds, i);
            if (string != null) {
                if (string.contains("0%")) {resume.append("\n").append(string);}
                logger.info("\n\n" + string + "\n");
                System.out.println(string);
            }
        }

        if (pathORAMChosen)
            Util.logAndPrint(logger, "Max stash size: " + oramFactory.getMaxStashSize() + ", max stash size between accesses: " + oramFactory.getMaxStashSizeBetweenAccesses());

        Util.logAndPrint(logger, "Overwriting with dummy blocks");
        if (communicationStrategy.sendEndSignal())
            Util.logAndPrint(logger, "Successfully rewrote all the blocks");
        else
            Util.logAndPrint(logger, "Unable to overwrite the blocks on the server");

        EncryptionStrategy encryptionStrategy = factory.getEncryptionStrategy();
        if (encryptionStrategy instanceof EncryptionStrategyTiming)
            Util.logAndPrint(logger, "Encryption time: " +
                    Util.getTimeString(((EncryptionStrategyTiming) encryptionStrategy).getTime() / 1000000));

        if (communicationStrategy instanceof CommunicationStrategyTiming)
            Util.logAndPrint(logger, "Communication time: " +
                    Util.getTimeString(((CommunicationStrategyTiming) communicationStrategy).getTime() / 1000000));

        Util.logAndPrint(logger, "\n ### Resume ###\n" + resume.toString());
    }

    private static ORAMFactory getOramFactory(String name) {
        Scanner scanner = new Scanner(System.in);
        Util.logAndPrint(logger, "Choose ORAM: " + name + " [l/p/t]");
        String answer = scanner.nextLine();
        while (!(answer.equals("l") || answer.equals("p") || answer.equals("t"))) {
            System.out.println("Choose ORAM [l/p/t]");
            answer = scanner.nextLine();
        }
        switch (answer) {
            case "l":
                return new ORAMFactoryLookahead();
            case "p":
                return new ORAMFactoryPath();
            case "t":
                return new ORAMFactoryTrivial();
            default:
                logger.error("Unable to find ORAM factory for: " + answer);
                System.exit(-1);
                return null;
        }
    }

    private static void printTreeFromServer(int size, int bucketSize, CommunicationStrategy com,
                                            AccessStrategyPath access, int offset) {
        BlockEncrypted[] array = new BlockEncrypted[size * bucketSize];
        for (int j = 0; j < array.length; j++)
            array[j] = com.read(j + offset);
        System.out.println(Util.printTree(array, bucketSize, access, Util.getEmptyStringOfLength(offset)));
    }
}
