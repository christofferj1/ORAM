package oram.factory;

import oram.clientcom.ClientCommunicationLayer;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyImpl;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 12-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class FactoryLookahead implements Factory {
    @Override
    public EncryptionStrategy getEncryptionStrategy() {
        return new EncryptionStrategyImpl();
    }

    @Override
    public CommunicationStrategy getCommunicationStrategy() {
        return new ClientCommunicationLayer();
    }
}
