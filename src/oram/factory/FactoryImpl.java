package oram.factory;

import oram.clientcom.CommunicationStrategy;
import oram.clientcom.CommunicationStrategyImpl;
import oram.clientcom.CommunicationStrategyTiming;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyImpl;
import oram.encryption.EncryptionStrategyTiming;
import oram.permutation.PermutationStrategy;
import oram.permutation.PermutationStrategyImpl;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 12-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class FactoryImpl implements Factory {
    private EncryptionStrategyImpl encryptionStrategy;
    private EncryptionStrategyTiming encryptionStrategyTiming;
    private CommunicationStrategyImpl communicationStrategyImpl;
    private CommunicationStrategyTiming communicationStrategyTiming;
    private PermutationStrategyImpl permutationStrategy;

    @Override
    public EncryptionStrategy getEncryptionStrategy() {
        if (encryptionStrategy == null) {
            encryptionStrategy = new EncryptionStrategyImpl();
            encryptionStrategyTiming = new EncryptionStrategyTiming(encryptionStrategy);
        }
        return encryptionStrategyTiming;
    }

    @Override
    public CommunicationStrategy getCommunicationStrategy() {
        if (communicationStrategyImpl == null) {
            communicationStrategyImpl = new CommunicationStrategyImpl();
            communicationStrategyTiming = new CommunicationStrategyTiming(communicationStrategyImpl);
        }
        return communicationStrategyTiming;
    }

    @Override
    public PermutationStrategy getPermutationStrategy() {
        if (permutationStrategy == null)
            permutationStrategy = new PermutationStrategyImpl();
        return permutationStrategy;
    }
}
