package oram.factory;

import oram.clientcom.ClientCommunicationLayer;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyImpl;
import oram.permutation.PermutationStrategy;
import oram.permutation.PermutationStrategyImpl;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 12-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class FactoryImpl implements Factory {
    private EncryptionStrategyImpl encryptionStrategy;
    private ClientCommunicationLayer clientCommunicationLayer;
    private PermutationStrategyImpl permutationStrategy;

    @Override
    public EncryptionStrategy getEncryptionStrategy() {
        if (encryptionStrategy == null)
            encryptionStrategy = new EncryptionStrategyImpl();
        return encryptionStrategy;
    }

    @Override
    public CommunicationStrategy getCommunicationStrategy() {
        if (clientCommunicationLayer == null)
            clientCommunicationLayer = new ClientCommunicationLayer();
        return clientCommunicationLayer;
    }

    @Override
    public PermutationStrategy getPermutationStrategy() {
        if (permutationStrategy == null)
            permutationStrategy = new PermutationStrategyImpl();
        return permutationStrategy;
    }
}
