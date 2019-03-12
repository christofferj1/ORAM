package oram.factory;

import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public interface Factory {
    EncryptionStrategy getEncryptionStrategy();

    CommunicationStrategy getCommunicationStrategy();
}
