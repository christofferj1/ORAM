package oram.ofactory;

import oram.AccessStrategy;
import oram.Util;
import oram.factory.Factory;
import oram.path.AccessStrategyPath;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class ORAMFactoryPath implements ORAMFactory {
    private AccessStrategyPath accessStrategy;
    private int size;
    private int bucketSize;
    private int numberOfBlocks;
    private int numberOfRounds;

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public AccessStrategy getAccessStrategy(byte[] secretKey, Factory factory) {
        if (accessStrategy == null)
            accessStrategy = new AccessStrategyPath(size, bucketSize, secretKey, factory);
        return accessStrategy;
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
        bucketSize = Util.getInteger("bucket size");
    }

    @Override
    public String getInitString() {
        return "Size: " + size + ", bucket size: " + bucketSize + ", with number of blocks: " + numberOfBlocks +
                ", doing rounds: " + numberOfRounds;
    }

    @Override
    public int getMaxStashSize() {
        return accessStrategy.getMaxStashSize();
    }

    @Override
    public int getMaxStashSizeBetweenAccesses() {
        return accessStrategy.getMaxStashSizeBetweenAccesses();
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
        return bucketSize;
    }

    @Override
    public int factorySizeParameter0() {
        return size;
    }

    @Override
    public int factorySizeParameter1() {
        return bucketSize;
    }
}
