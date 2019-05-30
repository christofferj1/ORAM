package oram.util;

import oram.blockenc.BlockEncryptionStrategyLookahead;
import oram.blockenc.BlockEncryptionStrategyPath;
import oram.clientcom.CommunicationStrategy;
import oram.clientcom.CommunicationStrategyStub;
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

    @Override
    public BlockEncryptionStrategyPath getBlockEncryptionStrategyPath() {
        return new BlockEncryptionStrategyPath(getEncryptionStrategy(), getPermutationStrategy());
    }

    @Override
    public BlockEncryptionStrategyLookahead getBlockEncryptionStrategyLookahead() {
        return new BlockEncryptionStrategyLookahead(getEncryptionStrategy());
    }

    public void setCommunicationStrategyStub(CommunicationStrategyStub communicationStrategyStub) {
        this.communicationStrategyStub = communicationStrategyStub;
    }
}
