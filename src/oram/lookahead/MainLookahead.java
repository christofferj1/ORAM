package oram.lookahead;

import oram.Constants;
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
import java.util.Arrays;
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
        byte[] key = new byte[Constants.AES_KEY_SIZE];
        SecureRandom randomness = new SecureRandom();
        randomness.nextBytes(key);

        int numberOfBlocks = 30;
        int columns = 8;
        int rows = 6;
        int size = 36;
        int numberOfRounds = 10000;

        BlockStandard[] blockArray = new BlockStandard[(numberOfBlocks + 1)];
        List<BlockStandard> blocks = new ArrayList<>();
        for (int i = 1; i <= numberOfBlocks; i++) {
            BlockStandard block = new BlockStandard(i, ("Block " + i).getBytes());
            blocks.add(block);
            blockArray[i] = block;
        }

        Factory factory = new FactoryCustom(Enc.IMPL, Com.STUB, Per.IMPL, columns, rows);

        CommunicationStrategy clientCommunicationLayer = factory.getCommunicationStrategy();
        clientCommunicationLayer.start();
        AccessStrategyLookahead access = new AccessStrategyLookahead(size, rows, key, factory);
        access.setup(blocks);

        printMatrix(columns, rows, clientCommunicationLayer, access);

//        System.out.println(clientCommunicationLayer.getMatrixAndStashString(access));

        List<Integer> addressesWrittenTo = new ArrayList<>();
        logger.info("Size: " + size + ", rows: " + rows + ", columns: " + columns + ", blocks: " + numberOfBlocks + ", rounds: " + numberOfRounds);
        for (int i = 0; i < numberOfRounds; i++) {
            OperationType op;
            int address;
            byte[] data;
            if (addressesWrittenTo.isEmpty() || randomness.nextBoolean()) {
                op = OperationType.WRITE;
                data = Util.getRandomString(8).getBytes();
                address = randomness.nextInt(numberOfBlocks) + 1;

                addressesWrittenTo.add(address);
            } else {
                op = OperationType.READ;
                data = null;
                address = addressesWrittenTo.get(randomness.nextInt(addressesWrittenTo.size()));
            }


            byte[] res = access.access(op, address, data);
            if (res == null) System.exit(-1);

            res = Util.removeTrailingZeroes(res);
            String s = new String(res);
            System.out.println("Accessed block " + StringUtils.leftPad(String.valueOf(address), 2) + ": " + StringUtils.leftPad(s, 8) + ", op type: " + op + ", data: " + (data != null ? new String(data) : null) + " in round: " + StringUtils.leftPad(String.valueOf(i), 4));

//            System.out.println(clientCommunicationLayer.getMatrixAndStashString(access));

            if (Arrays.equals(res, blockArray[address].getData())
                    || (op.equals(OperationType.WRITE) && Arrays.equals(res, Constants.DUMMY_RESPONSE.getBytes()))) {
                System.out.println("Read block data: " + s);
            } else {
                System.out.println("SHIT WENT WRONG!!! - WRONG BLOCK!!!");
                break;
            }

            if (op.equals(OperationType.WRITE)) blockArray[address] = new BlockStandard(address, data);
//            if (!s.contains(Integer.toString(address))) {
//                System.out.println("SHIT WENT WRONG!!!");
//                break;
//            }
            printMatrix(columns, rows, clientCommunicationLayer, access);
//            Util.printPercentageDone(startTime, numberOfRounds, i);
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
