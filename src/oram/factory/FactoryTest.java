package oram.factory;

import oram.CommunicationStrategyStub;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyImpl;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 12-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class FactoryTest implements Factory {
    private final int columns;
    private final int rows;

    public FactoryTest(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
    }


    @Override
    public EncryptionStrategy getEncryptionStrategy() {
        return new EncryptionStrategyImpl();
    }

    @Override
    public CommunicationStrategy getCommunicationStrategy() {
        return new CommunicationStrategyStub(columns, rows);
    }
}
