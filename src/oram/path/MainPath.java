package oram.path;

import oram.Constants;
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
        long startTime = System.nanoTime();
        byte[] key = new byte[Constants.AES_KEY_SIZE];
        SecureRandom randomness = new SecureRandom();
        randomness.nextBytes(key);

        int numberOfBlocks = 5;
        int bucketSize = 2;
        int size = 7;
        int numberOfRounds = 1000;

        List<BlockStandard> blocks = new ArrayList<>();
        BlockStandard[] blockArray = new BlockStandard[(numberOfBlocks + 1)];
        for (int i = 0; i < numberOfBlocks; i++) {
            int blockNumber = i + 1;
            BlockStandard block = new BlockStandard(blockNumber, ("Block " + blockNumber).getBytes());
            blocks.add(block);
            blockArray[i + 1] = block;
        }

        Factory factory = new FactoryCustom(Enc.IMPL, Com.IMPL, Per.IMPL, size, bucketSize);

        CommunicationStrategy com = factory.getCommunicationStrategy();
        com.start();
        AccessStrategyPath access = new AccessStrategyPath(size, bucketSize, key, factory);
        access.initializeServer(blocks);

        printTreeFromServer(size, bucketSize, com, access);
//        printTreeFromServer(bucketSize, com);
//        System.out.println(com.getTreeString());

        logger.info("Size: " + size + ", bucket size: " + bucketSize + ", doing rounds: " + numberOfRounds + ", with number of blocks: " + numberOfBlocks);
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
            logger.info("Accessed block " + StringUtils.leftPad(String.valueOf(address), 2) + ": " + StringUtils.leftPad(s, 8) + ", op type: " + op + ", data: " + (data != null ? new String(data) : null) + " in round: " + StringUtils.leftPad(String.valueOf(i), 4));

            printTreeFromServer(size, bucketSize, com, access);

            if (Arrays.equals(res, blockArray[address].getData())) {
//                System.out.println("Read block data: " + s);
            } else {
                System.out.println("SHIT WENT WRONG!!! - WRONG BLOCK!!!");
                break;
            }

            if (op.equals(OperationType.WRITE)) blockArray[address] = new BlockStandard(address, data);

//            System.out.println(com.getTreeString());
//            System.out.println(clientCommunicationLayer.getMatrixAndStashString(access));
//            if (!s.contains(Integer.toString(address))) {
//                System.out.println("SHIT WENT WRONG!!!");
//                break;
//            }
            if (i % 100 == 99)
                System.out.println("Done " + (i + 1) + "/" + numberOfRounds + " of the rounds");
        }
        System.out.println("Max stash size: " + access.maxStashSize);
        logger.info("Max stash size: " + access.maxStashSize);
        System.out.println("Max stash size between accesses: " + access.maxStashSizeBetweenAccesses);
        logger.info("Max stash size between accesses: " + access.maxStashSizeBetweenAccesses);

        long timeElapsed = (System.nanoTime() - startTime) / 1000000;

        System.out.println("Time: " + Util.getTimeString(timeElapsed));
    }

    private static void printTreeFromServer(int size, int bucketSize, CommunicationStrategy com, AccessStrategyPath access) {
        BlockEncrypted[] array = new BlockEncrypted[size * bucketSize];
        for (int j = 0; j < array.length; j++)
            array[j] = com.read(j);
        System.out.println(Util.printTree(array, bucketSize, access));
    }
}
