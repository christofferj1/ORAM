package oram.ofactory;

import oram.AccessStrategy;
import oram.Util;
import oram.factory.Factory;
import oram.lookahead.AccessStrategyLookahead;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class ORAMFactoryLookahead implements ORAMFactory {
    private int size;
    private int numberOfBlocks;
    private int numberOfRounds;
    private int rows;
    private int columns;
    private int offset;
    private int totalSize;

    public ORAMFactoryLookahead(int size, int offSet) {
        this.size = size;
        this.offset = offSet;
        numberOfBlocks = Math.min(size, 1024);
        rows = (int) Math.sqrt(size);
        columns = rows + 2;
        totalSize = (int) (size + 2 * Math.sqrt(size));
    }

    public ORAMFactoryLookahead() {
        size = Util.getInteger("size");
        numberOfBlocks = Math.min(size, 1024);
        numberOfRounds = Util.getInteger("number of rounds");
        rows = (int) Math.sqrt(size);
        columns = rows + 2;
        totalSize = (int) (size + 2 * Math.sqrt(size));
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public AccessStrategy getAccessStrategy(byte[] secretKey, Factory factory, AccessStrategy accessStrategy,
                                            int prefixSize) {
        return new AccessStrategyLookahead(size, rows, secretKey, factory, offset, accessStrategy, prefixSize);
    }

    @Override
    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    @Override
    public int getOffSet() {
        return offset;
    }

    @Override
    public String getInitString() {
        return "Lookahead, size: " + size + ", rows: " + rows + ", columns: " + columns + ", blocks: " + numberOfBlocks +
                ", rounds: " + numberOfRounds;
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
        return columns;
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public int getBucketSize() {
        return 0;
    }

    @Override
    public int factorySizeParameter0() {
        return rows;
    }

    @Override
    public int factorySizeParameter1() {
        return columns;
    }
}
