package oram.factory;

import oram.blockenc.BlockEncryptionStrategyLookahead;
import oram.blockenc.BlockEncryptionStrategyPath;
import oram.clientcom.CommunicationStrategy;
import oram.clientcom.CommunicationStrategyCounting;
import oram.clientcom.CommunicationStrategyStub;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyCounting;
import oram.encryption.EncryptionStrategyImpl;
import oram.ofactory.ORAMFactory;
import oram.permutation.PermutationStrategy;
import oram.permutation.PermutationStrategyImpl;

import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 12-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class FactoryLocal implements Factory {
    private final List<ORAMFactory> oramFactories;
    private final int numberOfORAMLayers;
    private EncryptionStrategyImpl encryptionStrategy;
    private EncryptionStrategyCounting encryptionStrategyCounting;
    private CommunicationStrategyStub communicationStrategyStub;
    private CommunicationStrategyCounting communicationStrategyCounting;
    private PermutationStrategyImpl permutationStrategy;

    public FactoryLocal(List<ORAMFactory> oramFactories, int numberOfORAMLayers) {
        this.oramFactories = oramFactories;
        this.numberOfORAMLayers = numberOfORAMLayers;
    }

    @Override
    public EncryptionStrategy getEncryptionStrategy() {
        if (encryptionStrategy == null) {
            encryptionStrategy = new EncryptionStrategyImpl();
            encryptionStrategyCounting = new EncryptionStrategyCounting(encryptionStrategy);
        }
        return encryptionStrategyCounting;
    }

    @Override
    public CommunicationStrategy getCommunicationStrategy() {
        if (communicationStrategyStub == null) {
            communicationStrategyStub = new CommunicationStrategyStub(oramFactories, numberOfORAMLayers);
            communicationStrategyCounting = new CommunicationStrategyCounting(communicationStrategyStub);
        }
        return communicationStrategyCounting;
    }

    @Override
    public PermutationStrategy getPermutationStrategy() {
        if (permutationStrategy == null)
            permutationStrategy = new PermutationStrategyImpl();
        return permutationStrategy;
    }

    @Override
    public BlockEncryptionStrategyPath getBlockEncryptionStrategyPath() {
        return new BlockEncryptionStrategyPath(getEncryptionStrategy(), getPermutationStrategy());
    }

    @Override
    public BlockEncryptionStrategyLookahead getBlockEncryptionStrategyLookahead() {
        return new BlockEncryptionStrategyLookahead(getEncryptionStrategy());
    }
}
