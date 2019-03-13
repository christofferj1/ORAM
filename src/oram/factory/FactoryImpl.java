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
    @Override
    public EncryptionStrategy getEncryptionStrategy() {
        return new EncryptionStrategyImpl();
    }

    @Override
    public CommunicationStrategy getCommunicationStrategy() {
        return new ClientCommunicationLayer();
    }

    @Override
    public PermutationStrategy getPermutationStrategy() {
        return new PermutationStrategyImpl();
    }
}
