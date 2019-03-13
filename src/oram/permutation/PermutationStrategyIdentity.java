package oram.permutation;

import oram.BlockEncrypted;
import oram.path.BlockStandard;

import java.util.ArrayList;
import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 13-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class PermutationStrategyIdentity implements PermutationStrategy {
    @Override
    public List<BlockEncrypted> permuteEncryptedBlocks(List<BlockEncrypted> blocks) {
        return new ArrayList<>(blocks);
    }

    @Override
    public List<BlockStandard> permuteStandardBlocks(List<BlockStandard> blocks) {
        return new ArrayList<>(blocks);
    }
}