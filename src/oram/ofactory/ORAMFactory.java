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

    int getColumns();

    int getRows();

    int getBucketSize();

    int factorySizeParameter0();

    int factorySizeParameter1();

    int getNumberOfRounds();

    void setNumberOfRounds(int numberOfRounds);

    int getNumberOfBlocks();

    int getOffSet();

    String getInitString();

    int getMaxStashSize();

    int getMaxStashSizeBetweenAccesses();
}
