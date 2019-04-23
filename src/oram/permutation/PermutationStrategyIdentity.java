package oram.permutation;

import oram.block.BlockEncrypted;
import oram.block.BlockLookahead;
import oram.block.BlockPath;
import oram.block.BlockTrivial;

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
    public List<BlockTrivial> permuteTrivialBlocks(List<BlockTrivial> blocks) {
        return new ArrayList<>(blocks);
    }

    @Override
    public List<BlockLookahead> permuteLookaheadBlocks(List<BlockLookahead> blocks) {
        return new ArrayList<>(blocks);
    }

    @Override
    public List<BlockPath> permutePathBlocks(List<BlockPath> blocks) {
        return new ArrayList<>(blocks);
    }
}
