package oram.lookahead;

import oram.AccessStrategy;
import oram.OperationType;
import oram.block.BlockTrivial;

import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 27-04-2019. <br>
 * Master Thesis 2019 </p>
 */

public class AccessStrategyDummy implements AccessStrategy {
    @Override
    public boolean setup(List<BlockTrivial> blocks) {
        return false;
    }

    @Override
    public byte[] access(OperationType op, int address, byte[] data, boolean recursiveLookup, boolean lookaheadSetup) {
        return new byte[0];
    }
}
