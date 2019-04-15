package oram;

import oram.block.BlockStandard;

import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 19-02-2019. <br>
 */

public interface AccessStrategy {
    boolean setup(List<BlockStandard> blocks);

    //    TODO: add an access method which takes a position (bytes for it) map as parameter, and sets it before it calls
//     the original access method
    byte[] access(OperationType op, int address, byte[] data, boolean recursiveLookup, boolean lookaheadSetup);
}
