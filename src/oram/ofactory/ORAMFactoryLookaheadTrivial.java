package oram.ofactory;

import oram.AccessStrategy;
import oram.Constants;
import oram.Util;
import oram.factory.Factory;
import oram.lookahead.AccessStrategyLookaheadTrivial;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class ORAMFactoryLookaheadTrivial implements ORAMFactory {
    private int size;
    private int numberOfBlocks;
    private int numberOfRounds;
    private int rows;
    private int columns;
    private int offset;
    private int totalSize;

    public ORAMFactoryLookaheadTrivial(int size, int offSet) {
        this.size = size;
        this.offset = offSet;
        numberOfBlocks = Math.min(size, 1000);
        rows = (int) Math.sqrt(size);
        columns = rows + 2;
        totalSize = (int) (size + 2 * Math.sqrt(size) + Math.ceil((double) size / Constants.POSITION_BLOCK_SIZE));
    }

    public ORAMFactoryLookaheadTrivial() {
        size = Util.getInteger("size, must be a square number");
        numberOfBlocks = Math.min(size, 1000);
        numberOfRounds = Util.getInteger("number of rounds");
        rows = (int) Math.sqrt(size);
        columns = rows + 2;
        totalSize = (int) (size + 2 * Math.sqrt(size) + Math.ceil((double) size / Constants.POSITION_BLOCK_SIZE));
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public AccessStrategy getAccessStrategy(byte[] secretKey, Factory factory, AccessStrategy accessStrategy,
                                            int prefixSize) {
        return new AccessStrategyLookaheadTrivial(size, rows, secretKey, factory, offset, accessStrategy, prefixSize);
    }

    @Override
    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    @Override
    public String getInitString() {
        return "Lookahead-Trivial, size: " + size + ", rows: " + rows + ", columns: " + columns + ", blocks: " +
                numberOfBlocks + ", rounds: " + numberOfRounds + ", block size: " + Constants.BLOCK_SIZE;
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

}
