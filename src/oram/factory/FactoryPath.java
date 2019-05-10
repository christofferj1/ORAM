package oram.factory;

import oram.CommunicationStrategyStub;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyIdentity;
import oram.ofactory.ORAMFactory;
import oram.permutation.PermutationStrategy;
import oram.permutation.PermutationStrategyImpl;

import java.util.Collections;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 12-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class FactoryPath implements Factory {
    private final ORAMFactory oramFactory;
    private EncryptionStrategy encryptionStrategy;
    private CommunicationStrategyStub communicationStrategyStub;
    private PermutationStrategyImpl permutationStrategy;

    public FactoryPath(ORAMFactory oramFactory) {
        this.oramFactory= oramFactory;
    }

    @Override
    public EncryptionStrategy getEncryptionStrategy() {
        if (encryptionStrategy == null)
            encryptionStrategy = new EncryptionStrategyIdentity();

        return encryptionStrategy;
    }

    @Override
    public CommunicationStrategy getCommunicationStrategy() {
        if (encryptionStrategy == null)
            encryptionStrategy = new EncryptionStrategyIdentity();

        if (communicationStrategyStub == null)
            communicationStrategyStub = new CommunicationStrategyStub(Collections.singletonList(oramFactory), 1,
                    encryptionStrategy);

        return communicationStrategyStub;
    }

    @Override
    public PermutationStrategy getPermutationStrategy() {
        if (permutationStrategy == null)
            permutationStrategy = new PermutationStrategyImpl();
        return permutationStrategy;
    }
}
