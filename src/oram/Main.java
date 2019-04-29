package oram;

import oram.block.BlockEncrypted;
import oram.block.BlockTrivial;
import oram.clientcom.CommunicationStrategy;
import oram.clientcom.CommunicationStrategyTiming;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyTiming;
import oram.factory.Factory;
import oram.factory.FactoryImpl;
import oram.lookahead.AccessStrategyLookahead;
import oram.lookahead.AccessStrategyLookaheadTrivial;
import oram.ofactory.*;
import oram.path.AccessStrategyPath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.*;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class Main {
    private static final Logger logger = LogManager.getLogger("log");

    public static void main(String[] args) {
        byte[] key = Constants.KEY_BYTES;


//        ORAMFactory oramFactory = getOramFactory("main");
//        Factory factory = new FactoryCustom(Enc.IMPL, Com.IMPL, Per.IMPL, oramFactory.factorySizeParameter0(),
//                oramFactory.factorySizeParameter1());
        List<ORAMFactory> oramFactories = getORAMFactories();
        Factory factory = new FactoryImpl();

        int numberOfBlocks = oramFactories.get(0).getNumberOfBlocks();

        BlockTrivial[] blockArray = new BlockTrivial[(numberOfBlocks + 1)];
        List<BlockTrivial> blocks = new ArrayList<>();
        for (int i = 1; i <= numberOfBlocks; i++) {
            int mapBeginning = (i - 1) * Constants.POSITION_BLOCK_SIZE + 1;
            Map<Integer, Integer> map = Util.getDummyMap(mapBeginning);
            BlockTrivial block = new BlockTrivial(i, Util.getByteArrayFromMap(map));
            blocks.add(block);
            blockArray[i] = block;
        }

        boolean pathORAMChosen = oramFactories.get(0) instanceof ORAMFactoryPath;
        CommunicationStrategy communicationStrategy = factory.getCommunicationStrategy();
        communicationStrategy.start();

//        Create recursive ORAM
//        ORAMFactory oramFactory2 = getOramFactory("2");
//        AccessStrategy access2 = oramFactory2.getAccessStrategy(key, factory, null);
//
//        ORAMFactory oramFactory1 = getOramFactory("1");
//        AccessStrategy access1 = oramFactory1.getAccessStrategy(key, factory, access2);
//
//        AccessStrategy access = oramFactory.getAccessStrategy(key, factory, access1);
//        if (oramFactory instanceof ORAMFactoryLookahead)
//            access.setup(blocks);

        List<AccessStrategy> accesses = Util.getAccessStrategies(oramFactories, key, factory);
        for (int i = accesses.size(); i > 0; i--) {
            AccessStrategy a = accesses.get(i - 1);
            if (a instanceof AccessStrategyLookahead || a instanceof AccessStrategyLookaheadTrivial)
                if (!a.setup(blocks))
                    return;
        }

        SecureRandom randomness = new SecureRandom();
        List<Integer> addresses = new ArrayList<>();
        int numberOfRounds = oramFactories.get(0).getNumberOfRounds();
        for (int i = 0; i < numberOfRounds / 2; i++)
            addresses.add(randomness.nextInt(numberOfBlocks) + 1);

//        StringBuilder resume = new StringBuilder(oramFactories.get(0).getInitString());
//        Util.logAndPrint(logger, resume.toString());
        StringBuilder resume = initializeStringBuilder(oramFactories);

//        for (int j = 0; j < oramFactories.size(); j++) {
//            if (accesses.get(j) instanceof AccessStrategyPath)
//                printTreeFromServer(oramFactories.get(j).getSize(), oramFactories.get(j).getBucketSize(), communicationStrategy, (AccessStrategyPath) accesses.get(j), oramFactories.get(j).getOffSet());
//        }
//        printTreeFromServer(oramFactory.getSize(), oramFactory.getBucketSize(), communicationStrategy, (AccessStrategyPath) access, oramFactory.getOffSet());
//        printTreeFromServer(oramFactory1.getSize(), oramFactory1.getBucketSize(), communicationStrategy, (AccessStrategyPath) access1, oramFactory1.getOffSet());

        List<Integer> addressesWrittenTo = new ArrayList<>();
        long speedTestTime = 0;
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

            byte[] res = accesses.get(0).access(op, address, data, false, false);
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

            if (op.equals(OperationType.WRITE)) blockArray[address] = new BlockTrivial(address, data);

//            System.out.println("Block array");
//            for (int j = 0; j < blockArray.length; j++) {
//                BlockTrivial b = blockArray[j];
//                System.out.println("  " + j + ": " + (b != null ? b.toStringShort() : ""));
//            }
//            System.out.println(" ");
//            for (int j = 0; j < oramFactories.size(); j++) {
//                if (accesses.get(j) instanceof AccessStrategyPath)
//                    printTreeFromServer(oramFactories.get(j).getSize(), oramFactories.get(j).getBucketSize(), communicationStrategy, (AccessStrategyPath) accesses.get(j), oramFactories.get(j).getOffSet());
//            }
//            printTreeFromServer(oramFactory.getSize(), oramFactory.getBucketSize(), communicationStrategy, (AccessStrategyPath) access, oramFactory.getOffSet());
//            printTreeFromServer(oramFactory1.getSize(), oramFactory1.getBucketSize(), communicationStrategy, (AccessStrategyPath) access1, oramFactory1.getOffSet());

            String percentString = Util.getPercentageDoneString(startTime + speedTestTime, numberOfRounds, i);
            if (percentString != null) {
                long tmp = communicationStrategy.speedTest();
                percentString += ", speed test: " + (tmp / 1000000) + " ms, " + (16 / (tmp / 1000000)) + " Mb/ms";

                if (tmp < 0)
                    break;
                else
                    speedTestTime += tmp;

                if (percentString.contains("0%"))
                    resume.append("\n").append(percentString);

                logger.info("\n\n" + percentString + "\n");
                System.out.println(percentString);
            }
        }

        if (pathORAMChosen)
            Util.logAndPrint(logger, "Max stash size: " + oramFactories.get(0).getMaxStashSize() + ", max stash size between accesses: " + oramFactories.get(0).getMaxStashSizeBetweenAccesses());

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
        Util.logAndPrint(logger, "Choose ORAM: " + name + " [l/lt/p/t]");
        String answer = scanner.nextLine();
        while (!(answer.equals("l") || answer.equals("p") || answer.equals("t") || answer.equals("lt"))) {
            System.out.println("Choose ORAM [l/lt/p/t]");
            answer = scanner.nextLine();
        }
        switch (answer) {
            case "l":
                return new ORAMFactoryLookahead();
            case "lt":
                return new ORAMFactoryLookaheadTrivial();
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

    private static List<ORAMFactory> getORAMFactories() {
        int numberOfORAMS = Util.getInteger("number of ORAMs");
        if (numberOfORAMS == 1)
            return Collections.singletonList(getOramFactory("ONLY ORAM"));

        if (numberOfORAMS > 5) {
            System.out.println("Can't create higher than 5 recursive ORAMs");
            System.exit(-1);
        }
        int offset = 0;
        List<ORAMFactory> factories = new ArrayList<>();
        outer:
        for (int i = 0; i < numberOfORAMS; i++) {
            int levelSize = Util.getLevelSize(i, numberOfORAMS - 1);
            switch (Util.chooseORAMType("ORAM number " + i + " [l/lt/p/t]")) {
                case "l":
                    factories.add(new ORAMFactoryLookahead(levelSize, offset));
                    offset += levelSize + 2 * Math.sqrt(levelSize);
                    break;
                case "lt":
                    factories.add(new ORAMFactoryLookaheadTrivial(levelSize, offset));
                    break outer;
                case "p":
                    factories.add(new ORAMFactoryPath(levelSize, offset));
                    offset += (levelSize - 1) * Constants.DEFAULT_BUCKET_SIZE;
                    break;
                default:
                    factories.add(new ORAMFactoryTrivial(levelSize, offset));
                    break outer;
            }
        }
        factories.get(0).setNumberOfRounds(Util.getInteger("number of rounds"));
        return factories;
    }

    private static void printTreeFromServer(int size, int bucketSize, CommunicationStrategy com,
                                            AccessStrategyPath access, int offset) {
        BlockEncrypted[] array = new BlockEncrypted[size * bucketSize];
        for (int j = 0; j < array.length; j++)
            array[j] = com.read(j + offset);
        System.out.println(Util.printTree(array, bucketSize, access, Util.getEmptyStringOfLength(15)));
    }

    private static StringBuilder initializeStringBuilder(List<ORAMFactory> factories) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < factories.size(); i++) {
            String string = Util.getEmptyStringOfLength(i * 5) + factories.get(i).getInitString();
            builder.append(string).append("\n");
            Util.logAndPrint(logger, string);
        }
        return builder;
    }
}
