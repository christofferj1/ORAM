package oram.server;

import oram.Block;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 20-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public interface Server {
    Block read(int address);

    boolean write(int address, Block block);
}
