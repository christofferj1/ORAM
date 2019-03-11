package oram.server;

import oram.BlockEncrypted;

import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 11-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public interface ServerApplication {
    List<BlockEncrypted> read(List<Integer> address);

    boolean write(List<String> addresses, List<byte[]> dataArrays);
}
