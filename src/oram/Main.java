package oram;

import oram.block.BlockStandard;
import oram.clientcom.CommunicationStrategy;
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
        long startTime = System.nanoTime();
        byte[] key = Constants.KEY_BYTES;

        ORAMFactory oramFactory = getOramFactory();
        Factory factory = new FactoryCustom(Enc.IMPL, Com.STUB, Per.IMPL, oramFactory.factorySizeParameter0(),
                oramFactory.factorySizeParameter1());

        int numberOfBlocks = oramFactory.getNumberOfBlocks();
        BlockStandard[] blockArray = new BlockStandard[(numberOfBlocks + 1)];
        List<BlockStandard> blocks = new ArrayList<>();
        for (int i = 1; i <= numberOfBlocks; i++) {
            BlockStandard block = new BlockStandard(i, ("Block " + i).getBytes());
            blocks.add(block);
            blockArray[i] = block;
        }

        CommunicationStrategy clientCommunicationLayer = factory.getCommunicationStrategy();
        clientCommunicationLayer.start();
        AccessStrategy access = oramFactory.getAccessStrategy(key, factory);
        if (!(oramFactory instanceof ORAMFactoryPath))
            access.setup(blocks);

        int size = oramFactory.getSize();
        int numberOfRounds = oramFactory.getNumberOfRounds();
        String string = "Size: " + size + ", doing rounds: " + numberOfRounds + ", with number of blocks: " + numberOfBlocks;
        if (access.getClass().getSimpleName().equals(AccessStrategyPath.class.getSimpleName()))
            string += ", bucket size: " + oramFactory.getBucketSize();
        logger.info(string);
        System.out.println(string);

        SecureRandom randomness = new SecureRandom();
        for (int i = 0; i < numberOfRounds; i++) {
            int address = randomness.nextInt(numberOfBlocks) + 1;

            OperationType op;
            byte[] data;
            if (randomness.nextBoolean()) {
                op = OperationType.READ;
                data = null;
            } else {
                op = OperationType.WRITE;
                data = Util.getRandomString(8).getBytes();
            }

            byte[] res = access.access(op, address, data);
            if (res == null) System.exit(-1);

            res = Util.removeTrailingZeroes(res);
            String s = new String(res);
//            System.out.println("Accessed block " + StringUtils.leftPad(String.valueOf(address), 2) + ": " + StringUtils.leftPad(s, 8) + ", op type: " + op + ", data: " + (data != null ? new String(data) : null) + " in round: " + StringUtils.leftPad(String.valueOf(i), 4));

            if (Arrays.equals(res, blockArray[address].getData())) {
//                System.out.println("Read block data: " + s);
            } else {
                System.out.println("SHIT WENT WRONG!!! - WRONG BLOCK!!!");
                break;
            }

            if (op.equals(OperationType.WRITE)) blockArray[address] = new BlockStandard(address, data);

            String percentageDoneString = Util.getPercentageDoneString(startTime, numberOfRounds, i);
            if (percentageDoneString != null)
                Util.logAndPrint(logger, percentageDoneString);
        }
    }

    private static ORAMFactory getOramFactory() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Choose ORAM [l/p/t]");
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
}
