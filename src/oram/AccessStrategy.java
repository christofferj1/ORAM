package oram;

import oram.block.BlockTrivial;

import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 19-02-2019. <br>
 */

public interface AccessStrategy {
    boolean setup(List<BlockTrivial> blocks);

    byte[] access(OperationType op, int address, byte[] data, boolean recursiveLookup, boolean lookaheadSetup);
}
