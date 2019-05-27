package oram.ofactory;

import oram.AccessStrategy;
import oram.Constants;
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

//    For recursive ORAM
    public ORAMFactoryPath(int size, int offset) {
        this.offset = offset;
        numberOfBlocks = Math.min(size, 1000);
        this.size = size - 1;
        bucketSize = DEFAULT_BUCKET_SIZE;
        totalSize = size * bucketSize;
    }

//    For local position map ORAM
    public ORAMFactoryPath() {
        size = Util.getInteger("size, must be a power of 2");
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
    public String getInitString() {
        return "Path, size: " + size + ", bucket size: " + bucketSize + ", with number of blocks: " + numberOfBlocks +
                ", rounds: " + numberOfRounds + ", block size: " + Constants.BLOCK_SIZE;
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
    public int getBucketSize() {
        return bucketSize;
    }

}
