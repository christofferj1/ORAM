package oram.server;

import oram.BlockEncrypted;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 20-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public interface Server {
    BlockEncrypted read(int address);

    boolean write(int address, BlockEncrypted block);

    String getTreeString();
}
