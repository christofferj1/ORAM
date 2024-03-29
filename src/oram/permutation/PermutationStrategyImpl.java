package oram.permutation;

import oram.block.BlockEncrypted;
import oram.block.BlockLookahead;
import oram.block.BlockPath;
import oram.block.BlockTrivial;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class PermutationStrategyImpl implements PermutationStrategy {
    @Override
    public List<BlockEncrypted> permuteEncryptedBlocks(List<BlockEncrypted> blocks) {
        if (blocks == null || blocks.isEmpty()) return blocks;

        SecureRandom randomness = new SecureRandom();
        Collections.shuffle(blocks, randomness);
        return blocks;
    }

    @Override
    public List<BlockTrivial> permuteTrivialBlocks(List<BlockTrivial> blocks) {
        if (blocks == null || blocks.isEmpty()) return blocks;

        SecureRandom randomness = new SecureRandom();
        Collections.shuffle(blocks, randomness);
        return blocks;
    }

    @Override
    public List<BlockLookahead> permuteLookaheadBlocks(List<BlockLookahead> blocks) {
        if (blocks == null || blocks.isEmpty()) return blocks;

        SecureRandom randomness = new SecureRandom();
        Collections.shuffle(blocks, randomness);
        return blocks;
    }

    @Override
    public List<BlockPath> permutePathBlocks(List<BlockPath> blocks) {
        if (blocks == null || blocks.isEmpty()) return blocks;

        SecureRandom randomness = new SecureRandom();
        Collections.shuffle(blocks, randomness);
        return blocks;
    }
}
