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
    private int offset;
    private int totalSize;

    public ORAMFactoryTrivial(int size, int offset) {
        this.size = size + 1;
        numberOfBlocks = Math.min(size, 1000);
        this.offset = offset;
        totalSize = size + 1;
    }

    public ORAMFactoryTrivial() {
        size = Util.getInteger("size");
        numberOfBlocks = Math.min(size, 1000);
        numberOfRounds = Util.getInteger("number of rounds");
        offset = 0;
        totalSize = size + 1;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public AccessStrategy getAccessStrategy(byte[] secretKey, Factory factory, AccessStrategy accessStrategy,
                                            int prefixSize) {
        return new AccessStrategyTrivial(size, secretKey, factory, offset, prefixSize);
    }

    @Override
    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    @Override
    public int getOffSet() {
        return 0;
    }

    @Override
    public String getInitString() {
        return "Trivial, size: " + size + ", blocks: " + numberOfBlocks + ", rounds: " + numberOfRounds;
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
    public int getTotalSize() {
        return totalSize;
    }

    @Override
    public int getNumberOfRounds() {
        return numberOfRounds;
    }

    @Override
    public void setNumberOfRounds(int numberOfRounds) {
        this.numberOfRounds = numberOfRounds;
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
