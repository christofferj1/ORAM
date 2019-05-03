package oram.ofactory;

import oram.AccessStrategy;
import oram.Util;
import oram.factory.Factory;
import oram.path.AccessStrategyPath;

import static oram.Constants.DEFAULT_BUCKET_SIZE;

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
    private int offset;
    private int totalSize;

    public ORAMFactoryPath(int size, int offset) {
        this.offset = offset;
        numberOfBlocks = Math.min(size, 1000);
        this.size = size - 1;
        bucketSize = DEFAULT_BUCKET_SIZE;
        totalSize = size * bucketSize;
    }

    public ORAMFactoryPath() {
        size = Util.getInteger("size");
        numberOfBlocks = Math.min(size, 1000);
        numberOfRounds = Util.getInteger("number of rounds");
        bucketSize = Util.getInteger("bucket size");
        offset = 0;
        totalSize = size * bucketSize;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public AccessStrategy getAccessStrategy(byte[] secretKey, Factory factory, AccessStrategy accessStrategy,
                                            int prefixSize) {
        if (this.accessStrategy == null)
            this.accessStrategy = new AccessStrategyPath(size, bucketSize, secretKey, factory, accessStrategy, offset,
                    prefixSize);
        return this.accessStrategy;
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
