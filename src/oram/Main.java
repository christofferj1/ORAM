package oram;

import oram.block.BlockStandard;
import oram.clientcom.CommunicationStrategy;
import oram.factory.Factory;
import oram.factory.FactoryCustom;
import oram.ofactory.ORAMFactory;
import oram.ofactory.ORAMFactoryLookahead;
import oram.path.AccessStrategyPath;
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
 * Created by Christoffer S. Jensen on 28-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class Main {
    private static final Logger logger = LogManager.getLogger("log");

    public static void main(String[] args) {
        long startTime = System.nanoTime();
        byte[] key = new byte[Constants.AES_KEY_SIZE];
        SecureRandom randomness = new SecureRandom();
        randomness.nextBytes(key);

        ORAMFactory oramFactory = new ORAMFactoryLookahead();
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
        access.setup(blocks);

        int size = oramFactory.getSize();
        int numberOfRounds = oramFactory.getNumberOfRounds();
        String string = "Size: " + size + ", #rounds: " + numberOfRounds + ", #blocks: " + numberOfBlocks;
        if (access.getClass().getSimpleName().equals(AccessStrategyPath.class.getSimpleName()))
            string += ", bucket size: " + oramFactory.getBucketSize();
        logger.info(string);
        System.out.println(string);

        List<Integer> addressesWrittenTo = new ArrayList<>();
        for (int i = 0; i < numberOfRounds; i++) {
            OperationType op;
            byte[] data;
            if (addressesWrittenTo.isEmpty() || randomness.nextBoolean()) {
                op = OperationType.WRITE;
                data = Util.getRandomString(8).getBytes();
            } else {
                op = OperationType.READ;
                data = null;
            }

            int address;
            if (op.equals(OperationType.WRITE)) {
                address = randomness.nextInt(numberOfBlocks) + 1;
                addressesWrittenTo.add(address);
            } else
                address = addressesWrittenTo.get(randomness.nextInt(addressesWrittenTo.size()));

            byte[] res = access.access(op, address, data);
            if (res == null) System.exit(-1);

            res = Util.removeTrailingZeroes(res);
            String s = new String(res);
            System.out.println("Accessed block " + StringUtils.leftPad(String.valueOf(address), 2) + ": " + StringUtils.leftPad(s, 8) + ", op type: " + op + ", data: " + (data != null ? new String(data) : null) + " in round: " + StringUtils.leftPad(String.valueOf(i), 4));

            if (Arrays.equals(res, blockArray[address].getData())
                    || (op.equals(OperationType.WRITE) && Arrays.equals(res, Constants.DUMMY_RESPONSE.getBytes()))) {
                System.out.println("Read block data: " + s);
            } else {
                System.out.println("SHIT WENT WRONG!!! - WRONG BLOCK!!!");
                break;
            }

            if (op.equals(OperationType.WRITE)) blockArray[address] = new BlockStandard(address, data);

            Util.printPercentageDone(startTime, numberOfRounds, i);
        }
    }
}
