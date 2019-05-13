package oram;

import oram.block.BlockEncrypted;
import oram.block.BlockTrivial;
import oram.clientcom.CommunicationStrategy;
import oram.clientcom.CommunicationStrategyCounting;
import oram.clientcom.CommunicationStrategyTiming;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyTiming;
import oram.factory.Factory;
import oram.factory.FactoryImpl;
import oram.lookahead.AccessStrategyLookahead;
import oram.ofactory.ORAMFactory;
import oram.ofactory.ORAMFactoryPath;
import oram.path.AccessStrategyPath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class MainContinue {
    private static final Logger logger = LogManager.getLogger("log");

    public static void main(String[] args) {
        byte[] key = Constants.KEY_BYTES;


//        ORAMFactory oramFactory = getOramFactory("main");
//        Factory factory = new FactoryCustom(Enc.IMPL, Com.IMPL, Per.IMPL, oramFactory.factorySizeParameter0(),
//                oramFactory.factorySizeParameter1());

        List<ORAMFactory> oramFactories = new ArrayList<>();

        int size = 1024;
        ORAMFactoryPath oramFactoryPath0 = new ORAMFactoryPath(size, 0);
        ORAMFactoryPath oramFactoryPath1 = new ORAMFactoryPath(size, 0);
        ORAMFactoryPath oramFactoryPath2 = new ORAMFactoryPath(size, 0);
        oramFactories.add(oramFactoryPath0);
        oramFactories.add(oramFactoryPath1);
        oramFactories.add(oramFactoryPath2);

//        int size2 = 64;
//        ORAMFactoryLookahead oramFactoryLookahead2 = new ORAMFactoryLookahead(size2, 0);
//        oramFactories.add(oramFactoryLookahead2);
//        ORAMFactoryPath oramFactoryPath2 = new ORAMFactoryPath(size2, 0);
//        oramFactories.add(oramFactoryPath2);
//        ORAMFactoryTrivial oramFactoryTrivial2 = new ORAMFactoryTrivial(size2, 0);
//        oramFactories.add(oramFactoryTrivial2);

//        int size3 = 16;
//        ORAMFactoryLookahead oramFactoryLookahead3 = new ORAMFactoryLookahead(size3, 0);
//        oramFactories.add(oramFactoryLookahead3);
//        ORAMFactoryPath oramFactoryPath3 = new ORAMFactoryPath(size3, 0);
//        oramFactories.add(oramFactoryPath3);
//        ORAMFactoryTrivial oramFactoryTrivial3 = new ORAMFactoryTrivial(size3, 0);
//        oramFactories.add(oramFactoryTrivial3);

//        int[] numberOfBlocksArray = new int[9];
//        numberOfBlocksArray[0] = Math.min(size2, 100);
//        numberOfBlocksArray[1] = Math.min(size3, 100);

//        int[] numberOfRoundsArray = new int[3];
//        numberOfRoundsArray[0] = 4000;
//        numberOfRoundsArray[1] = 4000;
//        numberOfRoundsArray[2] = 1000;

        int[] blockSize = new int[3];
        blockSize[0] = 512;
        blockSize[1] = 65536;
        blockSize[2] = 262144;

        int numberOfRounds = Util.getInteger("number of rounds");
        for (int j = 0; j < oramFactories.size(); j++) {
            Constants.BLOCK_SIZE = blockSize[j];
//            int numberOfRounds = numberOfRoundsArray[j % 3];
            ORAMFactory oramFactory = oramFactories.get(j);
            oramFactory.setNumberOfRounds(numberOfRounds);
            Factory factory = new FactoryImpl();

            int numberOfBlocks = 100;
//            int numberOfBlocks = numberOfBlocksArray[j];
            BlockTrivial[] blockArray = new BlockTrivial[(numberOfBlocks + 1)];
            List<BlockTrivial> blocks = new ArrayList<>();
            for (int i = 1; i <= numberOfBlocks; i++) {
                BlockTrivial block = new BlockTrivial(i, new byte[Constants.BLOCK_SIZE]);
                blocks.add(block);
                blockArray[i] = block;
            }

            boolean pathORAMChosen = oramFactory instanceof ORAMFactoryPath;
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

            AccessStrategy access = oramFactory.getAccessStrategy(key, factory, null, 0);
            if (access instanceof AccessStrategyLookahead)
                if (!access.setup(blocks))
                    return;

            SecureRandom randomness = new SecureRandom();
            List<Integer> addresses = new ArrayList<>();

            for (int i = 0; i < numberOfRounds / 2; i++)
                addresses.add(randomness.nextInt(numberOfBlocks) + 1);

//        StringBuilder resume = new StringBuilder(oramFactories.get(0).getInitString());
//        Util.logAndPrint(logger, resume.toString());
            StringBuilder resume = new StringBuilder(oramFactory.getInitString());
            resume.append("\n");
            Util.logAndPrint(logger, oramFactory.getInitString());


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

                byte[] res = access.access(op, address, data, false, false);
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
                    if (!(communicationStrategy instanceof CommunicationStrategyCounting))
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

            Util.logAndPrint(logger, "\n ### Resume ###\n" + resume.toString() + "\n");

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.out.println("Got interrupted");
                logger.error("Sleeping thread got interrupted");
            }
        }
    }

    private static void printTreeFromServer(int size, int bucketSize, CommunicationStrategy com,
                                            AccessStrategyPath access, int offset) {
        BlockEncrypted[] array = new BlockEncrypted[size * bucketSize];
        for (int j = 0; j < array.length; j++)
            array[j] = com.read(j + offset);
        System.out.println(Util.printTree(array, bucketSize, access, Util.getEmptyStringOfLength(15)));
    }
}
