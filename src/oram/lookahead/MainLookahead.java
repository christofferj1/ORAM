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
        byte[] key = Constants.KEY_BYTES;

        int numberOfBlocks = 70;
        int columns = 11;
        int rows = 9;
        int size = 81;
        int numberOfRounds = 10000;

        BlockStandard[] blockArray = new BlockStandard[(numberOfBlocks + 1)];
        List<BlockStandard> blocks = new ArrayList<>();
        for (int i = 1; i <= numberOfBlocks; i++) {
            BlockStandard block = new BlockStandard(i, ("Block " + i).getBytes());
            blocks.add(block);
            blockArray[i] = block;
        }

        Factory factory = new FactoryCustom(Enc.IMPL, Com.IMPL, Per.IMPL, columns, rows);

        CommunicationStrategy clientCommunicationLayer = factory.getCommunicationStrategy();
        clientCommunicationLayer.start();
        AccessStrategyLookahead access = new AccessStrategyLookahead(size, rows, key, factory);
        access.setup(blocks);

//        printMatrix(columns, rows, clientCommunicationLayer, access);

        SecureRandom randomness = new SecureRandom();
        List<Integer> addresses = new ArrayList<>();
        for (int i = 0; i < numberOfRounds; i++)
            addresses.add(randomness.nextInt(numberOfBlocks) + 1);

        logger.info("Size: " + size + ", rows: " + rows + ", columns: " + columns + ", blocks: " + numberOfBlocks + ", rounds: " + numberOfRounds);
        long startTime = System.nanoTime();
        for (int i = 0; i < numberOfRounds; i++) {
//            printMatrix(columns, rows, clientCommunicationLayer, access);
            int address = addresses.get(i);

            OperationType op;
            byte[] data;
            if (randomness.nextBoolean()) {
                op = OperationType.READ;
                data = null;
            } else {
                op = OperationType.WRITE;
                data = Util.getRandomString(Constants.BLOCK_SIZE).getBytes();
            }

            byte[] res = access.access(op, address, data);
            if (res == null) System.exit(-1);

            //res = Util.removeTrailingZeroes(res); TODO: Why did I do that?
//            System.out.println("Accessed block " + StringUtils.leftPad(String.valueOf(address), 2) + ": " + StringUtils.leftPad(s, 8) + ", op type: " + op + ", data: " + (data != null ? new String(data) : null) + " in round: " + StringUtils.leftPad(String.valueOf(i), 4));

            if (!Arrays.equals(res, blockArray[address].getData())) {
                System.out.println("SHIT WENT WRONG!!! - WRONG BLOCK!!!");
//                printMatrix(columns, rows, clientCommunicationLayer, access);
                break;
            } else {
//                System.out.println("Read block data: " + s);
//                printMatrix(columns, rows, clientCommunicationLayer, access);
//                System.out.println("_________________________________________________________________________________");
            }

            if (op.equals(OperationType.WRITE)) blockArray[address] = new BlockStandard(address, data);

            String string = Util.getPercentageDoneString(startTime, numberOfRounds, i);
            if (string != null)
                Util.logAndPrint(logger, string);
        }

        System.out.println("Overwriting with dummy blocks");
        if (clientCommunicationLayer.sendEndSignal())
            System.out.println("Successfully rewrote all the blocks");
        else
            System.out.println("Unable to overwrite the blocks on the server");
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
