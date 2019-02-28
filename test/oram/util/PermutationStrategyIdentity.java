package oram.util;

import oram.BlockEncrypted;
import oram.permutation.PermutationStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class PermutationStrategyIdentity implements PermutationStrategy {
    @Override
    public List<BlockEncrypted> permuteBlocks(List<BlockEncrypted> blocks) {
        return new ArrayList<>(blocks);
    }
}
