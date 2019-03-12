package oram.server;

import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 11-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public interface ServerApplication {
    List<BlockServer> read(List<String> address);

    boolean write(List<String> addresses, List<byte[]> dataArrays);
}
