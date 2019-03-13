package oram.lookahead;

import oram.OperationType;
import oram.clientcom.CommunicationStrategy;
import oram.factory.Factory;
import oram.factory.FactoryTest;
import oram.path.BlockStandard;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 12-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class MainLookahead {
    public static void main(String[] args) {
        byte[] key = new byte[16];
        new SecureRandom().nextBytes(key);

        BlockStandard block1 = new BlockStandard(1, "Block 0".getBytes());
        BlockStandard block2 = new BlockStandard(2, "Block 1".getBytes());
        BlockStandard block3 = new BlockStandard(3, "Block 2".getBytes());
        BlockStandard block4 = new BlockStandard(4, "Block 3".getBytes());
        BlockStandard block5 = new BlockStandard(5, "Block 4".getBytes());
        BlockStandard block6 = new BlockStandard(6, "Block 5".getBytes());
        BlockStandard block7 = new BlockStandard(7, "Block 6".getBytes());
        BlockStandard block8 = new BlockStandard(8, "Block 7".getBytes());
        BlockStandard block9 = new BlockStandard(9, "Block 8".getBytes());
        BlockStandard block10 = new BlockStandard(10, "Block 9".getBytes());
        BlockStandard block11 = new BlockStandard(11, "Block 10".getBytes());
        BlockStandard block12 = new BlockStandard(12, "Block 11".getBytes());
        BlockStandard block13 = new BlockStandard(13, "Block 12".getBytes());
        BlockStandard block14 = new BlockStandard(14, "Block 13".getBytes());
        List<BlockStandard> blocks = new ArrayList<>(Arrays.asList(block1, block2, block3, block4, block5, block6,
                block7, block8, block9, block10, block11, block12, block13, block14));

        Factory factory = new FactoryTest(4, 6); // Sizes the server, matrix or tree
//        Factory factory = new FactoryImpl();

        CommunicationStrategy clientCommunicationLayer = factory.getCommunicationStrategy();
        clientCommunicationLayer.start();
        AccessStrategyLookahead access = new AccessStrategyLookahead(16, 4, key, factory);
        access.setup(blocks);

        byte[] res = access.access(OperationType.READ, 4, null);
        System.out.println("Read block 4: " + new String(res));

        res = access.access(OperationType.WRITE, 11, "Hello world".getBytes());
        System.out.println("Written block 11: " + new String(res));

        res = access.access(OperationType.READ, 11, null);
        System.out.println("Read block 11: " + new String(res));
    }
}
