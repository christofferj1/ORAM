package oram.blockcreator;

import oram.block.BlockEncrypted;

import java.util.List;

/**
 * <p> oram_server <br>
 * Created by Christoffer S. Jensen on 04-04-2019. <br>
 * Master Thesis 2019 </p>
 */

public interface BlockCreator {
    List<BlockEncrypted> createBlocks(List<String> addresses);
}
