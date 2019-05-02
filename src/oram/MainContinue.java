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

public class MainContinue {
    private static final Logger logger = LogManager.getLogger("log");

    public static void main(String[] args) {
        byte[] key = Constants.KEY_BYTES;


//        ORAMFactory oramFactory = getOramFactory("main");
//        Factory factory = new FactoryCustom(Enc.IMPL, Com.IMPL, Per.IMPL, oramFactory.factorySizeParameter0(),
//                oramFactory.factorySizeParameter1());

        List<ORAMFactory> oramFactories = new ArrayList<>();
        int size = Util.getInteger("size");
        ORAMFactoryLookahead oramFactoryLookahead = new ORAMFactoryLookahead(size, 0);
        oramFactories.add(oramFactoryLookahead);
        ORAMFactoryPath oramFactoryPath = new ORAMFactoryPath(size, 0);
        oramFactories.add(oramFactoryPath);
        ORAMFactoryTrivial oramFactoryTrivial = new ORAMFactoryTrivial(size, 0);
        oramFactories.add(oramFactoryTrivial);

        int numberOfBlocks = Math.min(size, 1000);
        int numberOfRounds = Util.getInteger("number of rounds");

        for (int j = 0; j < oramFactories.size(); j++) {
            ORAMFactory oramFactory = oramFactories.get(j);
            Factory factory = new FactoryImpl();

            BlockTrivial[] blockArray = new BlockTrivial[(numberOfBlocks + 1)];
            List<BlockTrivial> blocks = new ArrayList<>();
            for (int i = 1; i <= numberOfBlocks; i++) {
                int mapBeginning = (i - 1) * Constants.POSITION_BLOCK_SIZE + 1;
                Map<Integer, Integer> map = Util.getDummyMap(mapBeginning);
                BlockTrivial block = new BlockTrivial(i, Util.getByteArrayFromMap(map));
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
