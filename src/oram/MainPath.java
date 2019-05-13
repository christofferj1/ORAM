package oram;

import oram.block.BlockTrivial;
import oram.clientcom.CommunicationStrategy;
import oram.factory.Factory;
import oram.factory.FactoryPath;
import oram.ofactory.ORAMFactory;
import oram.ofactory.ORAMFactoryPath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class MainPath {
    private static final Logger logger = LogManager.getLogger("log");

    public static void main(String[] args) {
        byte[] key = Constants.KEY_BYTES;
        int numberOfRounds = Util.getInteger("number of rounds");

//        for (int z = 1; z <= 5; z++) {
        Constants.DEFAULT_BUCKET_SIZE = Util.getInteger("bucket size");
//            for (int rounds = 0; rounds < 5; rounds++) {
        Util.logAndPrint(logger, "Bucket size: " + Constants.DEFAULT_BUCKET_SIZE);

        ORAMFactory oramFactory = new ORAMFactoryPath(numberOfRounds);
        Factory factory = new FactoryPath(oramFactory);

        int numberOfBlocks = oramFactory.getNumberOfBlocks();

        BlockTrivial[] blockArray = new BlockTrivial[(numberOfBlocks + 1)];

        CommunicationStrategy communicationStrategy = factory.getCommunicationStrategy();
        communicationStrategy.start();

        AccessStrategy access = oramFactory.getAccessStrategy(key, factory, null, 0);

        List<Integer> addresses = IntStream.range(0, numberOfBlocks).boxed().collect(Collectors.toList());

        StringBuilder resume = initializeStringBuilder(Collections.singletonList(oramFactory));

        SecureRandom randomness = new SecureRandom();
        List<Integer> addressesWrittenTo = new ArrayList<>();
        long startTime = System.nanoTime();
        for (int i = 0; i < numberOfRounds; i++) {
            int address = addresses.get(i % addresses.size());

            boolean writing = randomness.nextBoolean();
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

            if (addressesWrittenTo.contains(address)) {
                if (res.length == 0)
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

            String percentString = Util.getPercentageDoneString(startTime, numberOfRounds, i);
            if (percentString != null) {
                if (percentString.contains("0%"))
                    resume.append("\n").append(percentString);

//                    logger.info("\n\n" + percentString + "\n");
//                    System.out.println(percentString);
            }
        }

        Util.logAndPrint(logger, "\n   ***************\n           Max stash size: " + oramFactory.getMaxStashSize() + ", max stash size between accesses: " + oramFactory.getMaxStashSizeBetweenAccesses() + "\n   ***************");

        Util.logAndPrint(logger, "\n ### Resume ###\n" + resume.toString());
//            }
//        }
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
