package oram.permutation;

import oram.BlockEncrypted;

import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public interface PermutationStrategy {
    List<BlockEncrypted> permuteBlocks(List<BlockEncrypted> blocks);
}
