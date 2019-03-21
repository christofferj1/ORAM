package oram.clientcom;

import oram.block.BlockEncrypted;

import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 20-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public interface CommunicationStrategy {
    boolean start();

    BlockEncrypted read(int address); // TODO should not be used by the strategies

    boolean write(int address, BlockEncrypted block); // TODO should not be used by the strategies

    List<BlockEncrypted> readArray(List<Integer> addresses);

    boolean writeArray(List<Integer> addresses, List<BlockEncrypted> blocks);
}
