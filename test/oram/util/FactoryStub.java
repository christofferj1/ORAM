package oram.util;

import oram.CommunicationStrategyStub;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyImpl;
import oram.factory.Factory;
import oram.permutation.PermutationStrategy;
import oram.permutation.PermutationStrategyIdentity;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 12-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class FactoryStub implements Factory {
    private CommunicationStrategyStub communicationStrategyStub;
    private EncryptionStrategy encryptionStrategy;
    private PermutationStrategy permutationStrategy;

    public FactoryStub(CommunicationStrategyStub communicationStrategyStub) {
        this.communicationStrategyStub = communicationStrategyStub;
        encryptionStrategy = new EncryptionStrategyImpl(); // Used as default
        permutationStrategy = new PermutationStrategyIdentity(); // Used as default
    }

    @Override
    public EncryptionStrategy getEncryptionStrategy() {
        return encryptionStrategy;
    }

    public void setEncryptionStrategy(EncryptionStrategy encryptionStrategy) {
        this.encryptionStrategy = encryptionStrategy;
    }

    @Override
    public CommunicationStrategy getCommunicationStrategy() {
        return communicationStrategyStub;
    }

    @Override
    public PermutationStrategy getPermutationStrategy() {
        return permutationStrategy;
    }

    public void setPermutationStrategy(PermutationStrategy permutationStrategy) {
        this.permutationStrategy = permutationStrategy;
    }

    public void setCommunicationStrategyStub(CommunicationStrategyStub communicationStrategyStub) {
        this.communicationStrategyStub = communicationStrategyStub;
    }
}
