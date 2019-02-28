package oram.permutation;

import oram.BlockEncrypted;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class PermutationStrategyImpl implements PermutationStrategy {
    @Override
    public List<BlockEncrypted> permuteBlocks(List<BlockEncrypted> blocks) {
        if (blocks == null || blocks.isEmpty()) return blocks;

        SecureRandom randomness = new SecureRandom();

        List<BlockEncrypted> src = new ArrayList<>(blocks);
        List<BlockEncrypted> res = new ArrayList<>();

        for (int i = blocks.size() - 1; i >= 0; i--)
            res.add(i == 0 ? src.remove(0) : src.remove(randomness.nextInt(i)));

        return res;
    }
}
