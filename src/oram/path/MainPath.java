package oram.path;

import oram.OperationType;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockStandard;
import oram.clientcom.CommunicationStrategy;
import oram.factory.Factory;
import oram.factory.FactoryCustom;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
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
        long startTime = System.nanoTime();
        byte[] key = new byte[16];
        SecureRandom randomness = new SecureRandom();
        randomness.nextBytes(key);

        int numberOfBlocks = 103;
        List<BlockStandard> blocks = new ArrayList<>();
        for (int i = 0; i < numberOfBlocks; i++) {
            int blockNumber = i + 1;
            blocks.add(new BlockStandard(blockNumber, ("Block " + blockNumber).getBytes()));
        }

        int bucketSize = 2;
        int size = 127;
        Factory factory = new FactoryCustom(Enc.IMPL, Com.IMPL, Per.IMPL, size, bucketSize);

        CommunicationStrategy com = factory.getCommunicationStrategy();
        com.start();
        AccessStrategyPath access = new AccessStrategyPath(size, bucketSize, key, factory);
        access.initializeServer(blocks);

//        printTreeFromServer(bucketSize, com);
//        System.out.println(com.getTreeString());

        for (int i = 0; i < 1000; i++) {
            int address = randomness.nextInt(numberOfBlocks) + 1;

            byte[] res = access.access(OperationType.READ, address, null);

            if (res == null) System.exit(-1);
            res = Util.removeTrailingZeroes(res);
            String s = new String(res);
            System.out.println("Read block " + StringUtils.leftPad(String.valueOf(address), 3) + ": " + StringUtils.leftPad(s, 9) + ", in round: " + StringUtils.leftPad(String.valueOf(i), 4));
//            logger.info("Read block " + StringUtils.leftPad(String.valueOf(address), 2) + ": " + StringUtils.leftPad(s, 8) + ", in round: " + StringUtils.leftPad(String.valueOf(i), 4));

//            printTreeFromServer(bucketSize, com);

//            System.out.println(com.getTreeString());
//            System.out.println(clientCommunicationLayer.getMatrixAndStashString(access));
            if (!s.contains(Integer.toString(address))) {
                System.out.println("SHIT WENT WRONG!!!");
                break;
            }
        }
        System.out.println("Max stash size: " + access.maxStashSize);
        logger.info("Max stash size: " + access.maxStashSize);
        System.out.println("Max stash size between accesses: " + access.maxStashSizeBetweenAccesses);
        logger.info("Max stash size between accesses: " + access.maxStashSizeBetweenAccesses);

        System.out.println(((System.nanoTime() - startTime) / 1000000) + " milliseconds");
    }

    private static void printTreeFromServer(int bucketSize, CommunicationStrategy com) {
        BlockEncrypted[] array = new BlockEncrypted[15 * bucketSize];
        for (int j = 0; j < array.length; j++)
            array[j] = com.read(j);
        System.out.println(Util.printTree(array, bucketSize));
    }
}
