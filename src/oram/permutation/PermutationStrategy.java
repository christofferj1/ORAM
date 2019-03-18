package oram.permutation;

import oram.block.BlockEncrypted;
import oram.block.BlockStandard;

import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public interface PermutationStrategy {
    List<BlockEncrypted> permuteEncryptedBlocks(List<BlockEncrypted> blocks);

    List<BlockStandard> permuteStandardBlocks(List<BlockStandard> blocks);
}
