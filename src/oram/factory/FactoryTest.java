package oram.factory;

import oram.CommunicationStrategyStub;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyIdentity;
import oram.permutation.PermutationStrategy;
import oram.permutation.PermutationStrategyIdentity;

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
        return new EncryptionStrategyIdentity();
    }

    @Override
    public CommunicationStrategy getCommunicationStrategy() {
        return new CommunicationStrategyStub(columns, rows);
    }

    @Override
    public PermutationStrategy getPermutationStrategy() {
        return new PermutationStrategyIdentity();
    }
}
