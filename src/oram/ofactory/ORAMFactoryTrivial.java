package oram.ofactory;

import oram.AccessStrategy;
import oram.Util;
import oram.factory.Factory;
import oram.trivial.AccessStrategyTrivial;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class ORAMFactoryTrivial implements ORAMFactory {
    private int size;
    private int numberOfBlocks;
    private int numberOfRounds;

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
    public void setParameters() {
        size = Util.getInteger("size");
        numberOfBlocks = Util.getInteger("number of blocks");
        numberOfRounds = Util.getInteger("number of rounds");
    }

    @Override
    public String getInitString() {
        return "Size: " + size + ", blocks: " + numberOfBlocks + ", rounds: " + numberOfRounds;
    }

    @Override
    public int getMaxStashSize() {
        return -42;
    }

    @Override
    public int getMaxStashSizeBetweenAccesses() {
        return -42;
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
