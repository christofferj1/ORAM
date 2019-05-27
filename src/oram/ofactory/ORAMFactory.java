package oram.ofactory;

import oram.AccessStrategy;
import oram.factory.Factory;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public interface ORAMFactory {
    int getSize();

    AccessStrategy getAccessStrategy(byte[] secretKey, Factory factory, AccessStrategy accessStrategy, int prefixSize);

    int getBucketSize();

    int getNumberOfRounds();

    void setNumberOfRounds(int numberOfRounds);

    int getNumberOfBlocks();

    String getInitString();

    int getMaxStashSize();

    int getMaxStashSizeBetweenAccesses();

    int getTotalSize();
}
