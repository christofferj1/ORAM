package oram.permutation;

import oram.block.BlockEncrypted;
import oram.block.BlockLookahead;
import oram.block.BlockPath;
import oram.block.BlockTrivial;

import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public interface PermutationStrategy {
    List<BlockEncrypted> permuteEncryptedBlocks(List<BlockEncrypted> blocks);

    List<BlockTrivial> permuteTrivialBlocks(List<BlockTrivial> blocks);

    List<BlockLookahead> permuteLookaheadBlocks(List<BlockLookahead> blocks);

    List<BlockPath> permutePathBlocks(List<BlockPath> blocks);
}
