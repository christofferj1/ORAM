package oram.lookahead;

import oram.OperationType;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockLookahead;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static oram.factory.FactoryCustom.*;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 12-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class MainLookahead {
    private static final Logger logger = LogManager.getLogger("log");

    public static void main(String[] args) {
        long startTime = System.nanoTime();
        byte[] key = new byte[16];
        SecureRandom randomness = new SecureRandom();
        randomness.nextBytes(key);

        int numberOfBlocks = 15;
        List<BlockStandard> blocks = new ArrayList<>();
        for (int i = 0; i < numberOfBlocks; i++) {
            int blockNumber = i + 1;
            blocks.add(new BlockStandard(blockNumber, ("Block " + blockNumber).getBytes()));
        }

        int columns = 6;
        int rows = 4;
        int size = 16;
        Factory factory = new FactoryCustom(Enc.IMPL, Com.IMPL, Per.IMPL, columns, rows);

        CommunicationStrategy clientCommunicationLayer = factory.getCommunicationStrategy();
        clientCommunicationLayer.start();
        AccessStrategyLookahead access = new AccessStrategyLookahead(size, rows, key, factory);
        access.setup(blocks);

        printMatrix(columns, rows, clientCommunicationLayer, access);

//        System.out.println(clientCommunicationLayer.getMatrixAndStashString(access));

        int numberOfRounds = 100;
        logger.info("Size: " + size + ", rows: " + rows + ", columns: " + columns + ", blocks: " + numberOfBlocks + ", rounds: " + numberOfRounds);
        for (int i = 0; i < numberOfRounds; i++) {
            int address = randomness.nextInt(numberOfBlocks) + 1;

            byte[] res = access.access(OperationType.READ, address, null);
            if (res == null) { System.exit(-1);}

            res = Util.removeTrailingZeroes(res);
            String s = new String(res);
            System.out.println("Read block " + StringUtils.leftPad(String.valueOf(address), 2) + ": " + StringUtils.leftPad(s, 8) + ", in round: " + StringUtils.leftPad(String.valueOf(i), 4));

//            System.out.println(clientCommunicationLayer.getMatrixAndStashString(access));
            printMatrix(columns, rows, clientCommunicationLayer, access);

            if (!s.contains(Integer.toString(address))) {
                System.out.println("SHIT WENT WRONG!!!");
                break;
            }
        }

        long timeElapsed = (System.nanoTime() - startTime) / 1000000;

        System.out.println("Time: " + Util.getTimeString(timeElapsed));

//        byte[] res = access.access(OperationType.WRITE, 11, "Hello world".getBytes());
//        if (res == null)
//            System.exit(-1);
//        System.out.println("Written block 11: " + new String(res));
//        System.out.println(clientCommunicationLayer.getMatrixAndStashString());
//
//        res = access.access(OperationType.READ, 11, null);
//        if (res == null)
//            System.exit(-1);
//        System.out.println("Read block 11: " + new String(res));
//        System.out.println(clientCommunicationLayer.getMatrixAndStashString());
    }

    public static void printMatrix(int columns, int rows, CommunicationStrategy clientCommunicationLayer,
                                   AccessStrategyLookahead access) {
        List<Integer> addresses = IntStream.range(0, columns * rows).boxed().collect(Collectors.toList());
        List<BlockEncrypted> encryptedBlocks = clientCommunicationLayer.readArray(addresses);
        List<BlockLookahead> blockLookaheads = access.decryptLookaheadBlocks(encryptedBlocks);
        BlockLookahead[] blockLookaheadArray = new BlockLookahead[columns * rows];
        for (int j = 0; j < rows * columns; j++)
            blockLookaheadArray[j] = blockLookaheads.get(j);
        System.out.println(Util.getMatrixString(blockLookaheadArray, rows));
    }
}
