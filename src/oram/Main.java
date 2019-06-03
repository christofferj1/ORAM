package oram;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.*;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-03-2019. <br>
 * Master Thesis 2019 </p>
 */

class Main {
    private static final Logger logger = LogManager.getLogger("log");

    public static void main(String[] args) {
        byte[] key = Constants.KEY_BYTES; // Used for encryption

//        Initialise factories
        List<ORAMFactory> oramFactories = getORAMFactories();
        Factory factory = new FactoryImpl();

//        Initialise block array
        int numberOfBlocks = oramFactories.get(0).getNumberOfBlocks();
        BlockTrivial[] blockArray = new BlockTrivial[(numberOfBlocks + 1)];
        List<BlockTrivial> blocks = new ArrayList<>();
        for (int i = 1; i <= numberOfBlocks; i++) {
            int mapBeginning = (i - 1) * Constants.POSITION_BLOCK_SIZE + 1;
            Map<Integer, Integer> map = Util.getDummyMap(mapBeginning);
            BlockTrivial block = new BlockTrivial(i, Util.getByteArrayFromMap(map)); // Maps are used in case of recursive ORAMs
            blocks.add(block);
            blockArray[i] = block;
        }

//        Initialise communication
        CommunicationStrategy communicationStrategy = factory.getCommunicationStrategy();
        communicationStrategy.start(getIPString());

//        The Lookahead ORAM is initialised with the blocks it is going to use though out the execution
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

//        Initialise the string builder which gives a resume of the execution at the end
        StringBuilder resume = initializeStringBuilder(oramFactories);

//        Keeps track of the addresses written to, to know when the data read should be known and should match the local copy
        List<Integer> addressesWrittenTo = new ArrayList<>();
        long speedTestTime = 0;
        long startTime = System.nanoTime();
        for (int i = 0; i < numberOfRounds; i++) {
            int address = addresses.get(i % addresses.size());

            boolean writing = i < numberOfRounds / 2; // The first half of the rounds are writes, the rest are reads
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

            if (addressesWrittenTo.contains(address)) {
                if (oramFactories.get(0) instanceof ORAMFactoryPath && res.length == 0)
                    break;
                else { // When something goes south
                    if (!Arrays.equals(res, blockArray[address].getData())) {
                        Util.logAndPrint(logger, "Something went wrong! (wrong block)");
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

            speedTestTime += handleMessages(communicationStrategy, startTime, speedTestTime, numberOfRounds, i, resume);
        }

//        End the session
        Util.logAndPrint(logger, "Delete blocks on server");
        if (communicationStrategy.sendEndSignal())
            Util.logAndPrint(logger, "Successfully deleted all the blocks");
        else
            Util.logAndPrint(logger, "Unable to delete the blocks on the server");
        finalMessages(oramFactories, factory, communicationStrategy, resume);
    }

    private static String getIPString() {
        Scanner scanner = new Scanner(System.in);
        Util.logAndPrint(logger, "Provide an IP address to connect to (format: xxx.xxx.xxx.xxx)");
        String res = scanner.nextLine();
        while (!res.matches("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})")) {
            Util.logAndPrint(logger, "The IP address: " + res + " did not have the correct format");
            res = scanner.nextLine();
        }
        Util.logAndPrint(logger, "IP chosen: " + res);
        return res;
    }

    private static void finalMessages(List<ORAMFactory> oramFactories, Factory factory,
                                      CommunicationStrategy communicationStrategy, StringBuilder resume) {
        //        Print stash size, if main ORAM were Path ORAM
        if (oramFactories.get(0) instanceof ORAMFactoryPath)
            Util.logAndPrint(logger, "Max stash size: " + oramFactories.get(0).getMaxStashSize() +
                    ", max stash size between accesses: " + oramFactories.get(0).getMaxStashSizeBetweenAccesses());

//        Print the amount of time used to encrypt and decrypt blocks
        EncryptionStrategy encryptionStrategy = factory.getEncryptionStrategy();
        if (encryptionStrategy instanceof EncryptionStrategyTiming)
            Util.logAndPrint(logger, "Encryption time: " +
                    Util.getTimeString(((EncryptionStrategyTiming) encryptionStrategy).getTime() / 1000000));

//        Print the amount of time used to send and receive data
        if (communicationStrategy instanceof CommunicationStrategyTiming)
            Util.logAndPrint(logger, "Communication time: " +
                    Util.getTimeString(((CommunicationStrategyTiming) communicationStrategy).getTime() / 1000000));

//        Print the resume
        Util.logAndPrint(logger, "\n ### Resume ###\n" + resume.toString());
    }

    private static long handleMessages(CommunicationStrategy communicationStrategy, long startTime, long speedTestTime,
                                       int numberOfRounds, int roundNumber, StringBuilder resume) {
        String percentString = Util.getPercentageDoneString(startTime + speedTestTime, numberOfRounds, roundNumber);
        if (percentString != null) {
            long tmp = communicationStrategy.speedTest();
            percentString += ", speed test: " + (tmp / 1000000) + " ms, " + (16 / (tmp / 1000000)) + " Mb/ms";

            if (tmp < 0)
                return tmp;

            if (percentString.contains("0%"))
                resume.append("\n").append(percentString);

            logger.info("\n\n" + percentString + "\n");
            System.out.println(percentString);
            return tmp;
        }
        return 0;
    }

    private static List<ORAMFactory> getORAMFactories() {
        int numberOfORAMS = Util.getInteger("number of layers of ORAMs (between 1 and 5, both included)");
        if (numberOfORAMS == 1)
            return Collections.singletonList(getSingleOramFactory());

        if (numberOfORAMS > 5) {
            System.out.println("Can't create higher than 5 recursive ORAMs");
            System.exit(-1);
        }

        int offset = 0;
        List<ORAMFactory> factories = new ArrayList<>();
        outer:
        for (int i = 0; i < numberOfORAMS; i++) {
            int levelSize = Util.getLevelSize(i, numberOfORAMS - 1);
            switch (Util.chooseORAMType("ORAM number " + i + ", choose between Lookahead, Lookahead using Trivial specialised for Lookahead, Path, or Trivial [l/lt/p/t]")) {
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

    private static ORAMFactory getSingleOramFactory() {
        Scanner scanner = new Scanner(System.in);
        Util.logAndPrint(logger, "Choose ORAM, either Lookahead, Path or Trivial [l/p/t]");
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
