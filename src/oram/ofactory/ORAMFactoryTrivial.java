package oram.ofactory;

import oram.AccessStrategy;
import oram.factory.Factory;
import oram.trivial.AccessStrategyTrivial;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class ORAMFactoryTrivial implements ORAMFactory {
    private final int size = 36;
    private final int numberOfBlocks = size;
    private final int numberOfRounds = 10000;

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public AccessStrategy getAccessStrategy(byte[] secretKey, Factory factory) {
        return new AccessStrategyTrivial(size, secretKey, factory);
    }

    @Override
    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    @Override
    public int getNumberOfRounds() {
        return numberOfRounds;
    }

    @Override
    public int getColumns() {
        return 0;
    }

    @Override
    public int getRows() {
        return 0;
    }

    @Override
    public int getBucketSize() {
        return 0;
    }

    @Override
    public int factorySizeParameter0() {
        return size;
    }

    @Override
    public int factorySizeParameter1() {
        return 1;
    }

}
