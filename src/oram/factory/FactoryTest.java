package oram.factory;

import oram.CommunicationStrategyStub;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyImpl;
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
    private EncryptionStrategy encryptionStrategy;
    private CommunicationStrategyStub clientCommunicationLayer;
    private PermutationStrategyIdentity permutationStrategy;

    public FactoryTest(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
    }

    @Override
    public EncryptionStrategy getEncryptionStrategy() {
        if (encryptionStrategy == null)
            encryptionStrategy = new EncryptionStrategyImpl();
        return encryptionStrategy;
    }

    @Override
    public CommunicationStrategy getCommunicationStrategy() {
        if (clientCommunicationLayer == null)
            clientCommunicationLayer = new CommunicationStrategyStub(columns, rows);
        return clientCommunicationLayer;
    }

    @Override
    public PermutationStrategy getPermutationStrategy() {
        if (permutationStrategy == null)
            permutationStrategy = new PermutationStrategyIdentity();
        return permutationStrategy;
    }
}
