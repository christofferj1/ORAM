package oram.ofactory;

import oram.AccessStrategy;
import oram.factory.Factory;
import oram.lookahead.AccessStrategyLookahead;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class ORAMFactoryLookahead implements ORAMFactory {
    private final int size = 36;
    private final int numberOfBlocks = 30;
    private final int numberOfRounds = 100;
    private final int rows = 6;
    private final int columns = rows + 2;

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public AccessStrategy getAccessStrategy(byte[] secretKey, Factory factory) {
        return new AccessStrategyLookahead(size, rows, secretKey, factory);
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
