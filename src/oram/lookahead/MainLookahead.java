package oram.lookahead;

import oram.AccessStrategy;
import oram.OperationType;
import oram.clientcom.ClientCommunicationLayer;
import oram.clientcom.Server;

import java.util.Arrays;
import java.util.Random;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 12-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class MainLookahead {
    public static void main(String[] args) {
        byte[] key = new byte[16];
        new Random().nextBytes(key);

        Server clientCommunicationLayer = new ClientCommunicationLayer();
        AccessStrategy access = new AccessStrategyLookahead(16, 4, key, clientCommunicationLayer);

        byte[] res = access.access(OperationType.WRITE, 42, "Hello world".getBytes());
        System.out.println(Arrays.toString(res));

        res = access.access(OperationType.READ, 42, null);
        System.out.println(new String(res));
    }
}
