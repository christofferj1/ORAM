package oram.lookahead;

import oram.Constants;
import oram.OperationType;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockLookahead;
import oram.block.BlockStandard;
import oram.clientcom.CommunicationStrategy;
import oram.clientcom.CommunicationStrategyTiming;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyTiming;
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
        byte[] key = Constants.KEY_BYTES;

        int numberOfBlocks = 100;
        int columns = 12;
        int rows = 10;
        int size = 100;
        int numberOfRounds = 1000;

        BlockStandard[] blockArray = new BlockStandard[(numberOfBlocks + 1)];
        List<BlockStandard> blocks = new ArrayList<>();
        for (int i = 1; i <= numberOfBlocks; i++) {
            BlockStandard block = new BlockStandard(i, Util.getRandomByteArray(Constants.BLOCK_SIZE));
            blocks.add(block);
            blockArray[i] = block;
        }

        Factory factory = new FactoryCustom(Enc.IMPL, Com.IMPL, Per.IMPL, columns, rows);

        CommunicationStrategy communicationStrategy = factory.getCommunicationStrategy();
        communicationStrategy.start();
        AccessStrategyLookahead access = new AccessStrategyLookahead(size, rows, key, factory);
        access.setup(blocks);

        SecureRandom randomness = new SecureRandom();
        List<Integer> addresses = new ArrayList<>();
        for (int i = 0; i < numberOfRounds / 2; i++)
            addresses.add(randomness.nextInt(numberOfBlocks) + 1);

        StringBuilder resume = new StringBuilder("Size: " + size + ", rows: " + rows + ", columns: " + columns + ", blocks: " + numberOfBlocks + ", rounds: " + numberOfRounds);
        Util.logAndPrint(logger, resume.toString());

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

            byte[] res = access.access(op, address, data, false);
            if (res == null)
                break;

            //res = Util.removeTrailingZeroes(res); TODO: Why did I do that?
            logger.info("Accessed block " + StringUtils.leftPad(String.valueOf(address), 7) + ", op type: " + op + ", data: " + Util.getShortDataString(data) + " in round: " + StringUtils.leftPad(String.valueOf(i), 6) + ", returning data: " + Util.getShortDataString(res));

            if (!Arrays.equals(res, blockArray[address].getData())) {
                Util.logAndPrint(logger, "SHIT WENT WRONG!!! - WRONG BLOCK!!!");
                Util.logAndPrint(logger, "    The arrays, that weren't the same:");
                Util.logAndPrint(logger, "        res: " + Arrays.toString(res));
                Util.logAndPrint(logger, "        old: " + Arrays.toString(blockArray[address].getData()));
                Util.logAndPrint(logger, "    Block array");
                break;
            }

            if (op.equals(OperationType.WRITE)) blockArray[address] = new BlockStandard(address, data);

            String string = Util.getPercentageDoneString(startTime, numberOfRounds, i);
            if (string != null) {
                if (string.contains("0%"))
                    resume.append("\n").append(string);
                logger.info("\n\n" + string + "\n");
                System.out.println(string);
            }
        }

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

        Util.logAndPrint(logger, "\n ### Resume ###\n" + resume.toString());
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
