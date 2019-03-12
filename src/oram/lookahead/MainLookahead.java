package oram.lookahead;

import oram.OperationType;
import oram.clientcom.CommunicationStrategy;
import oram.factory.Factory;
import oram.factory.FactoryLookahead;
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
        List<BlockStandard> blocks = new ArrayList<>(Arrays.asList(block1, block2, block3, block4, block5, block6));

        Factory factory = new FactoryLookahead();

        CommunicationStrategy clientCommunicationLayer = factory.getCommunicationStrategy();
        clientCommunicationLayer.start();
        AccessStrategyLookahead access = new AccessStrategyLookahead(16, 4, key, factory);
        access.setup(blocks);

        byte[] res = access.access(OperationType.READ, 4, null);
        System.out.println(new String(res));

        res = access.access(OperationType.WRITE, 11, "Hello world".getBytes());
        System.out.println(Arrays.toString(res));

        res = access.access(OperationType.READ, 11, null);
        System.out.println(new String(res));
    }
}
