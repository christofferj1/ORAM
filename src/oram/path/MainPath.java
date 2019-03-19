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
        byte[] key = new byte[16];
        SecureRandom randomness = new SecureRandom();
        randomness.nextBytes(key);

        BlockStandard block1 = new BlockStandard(1, "Block  1".getBytes());
        BlockStandard block2 = new BlockStandard(2, "Block  2".getBytes());
        BlockStandard block3 = new BlockStandard(3, "Block  3".getBytes());
        BlockStandard block4 = new BlockStandard(4, "Block  4".getBytes());
        BlockStandard block5 = new BlockStandard(5, "Block  5".getBytes());
        BlockStandard block6 = new BlockStandard(6, "Block  6".getBytes());
        BlockStandard block7 = new BlockStandard(7, "Block  7".getBytes());
        BlockStandard block8 = new BlockStandard(8, "Block  8".getBytes());
        BlockStandard block9 = new BlockStandard(9, "Block  9".getBytes());
        BlockStandard block10 = new BlockStandard(10, "Block 10".getBytes());
        BlockStandard block11 = new BlockStandard(11, "Block 11".getBytes());
        BlockStandard block12 = new BlockStandard(12, "Block 12".getBytes());
        BlockStandard block13 = new BlockStandard(13, "Block 13".getBytes());
        BlockStandard block14 = new BlockStandard(14, "Block 14".getBytes());
        List<BlockStandard> blocks = new ArrayList<>(Arrays.asList(block1, block2, block3, block4, block5, block6,
                block7, block8, block9, block10, block11, block12, block13, block14));

        int bucketSize = 2;
        Factory factory = new FactoryCustom(Enc.IMPL, Com.IMPL, Per.IMPL, 15, bucketSize);

        CommunicationStrategy com = factory.getCommunicationStrategy();
        com.start();
        AccessStrategyPath access = new AccessStrategyPath(15, bucketSize, key, factory);
        access.initializeServer(blocks);

//        printTreeFromServer(bucketSize, com);
//        System.out.println(com.getTreeString());

        for (int i = 0; i < 100; i++) {
            int address = randomness.nextInt(14) + 1;

            byte[] res = access.access(OperationType.READ, address, null);

            if (res == null) System.exit(-1);
            res = Util.removeTrailingZeroes(res);
            String s = new String(res);
            System.out.println("Read block " + StringUtils.leftPad(String.valueOf(address), 2) + ": " + StringUtils.leftPad(s, 8) + ", in round: " + StringUtils.leftPad(String.valueOf(i), 4));
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
    }

    private static void printTreeFromServer(int bucketSize, CommunicationStrategy com) {
        BlockEncrypted[] array = new BlockEncrypted[15 * bucketSize];
        for (int j = 0; j < array.length; j++)
            array[j] = com.read(j);
        System.out.println(Util.printTree(array, bucketSize));
    }
}
