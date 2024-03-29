package oram.factory;

import oram.blockenc.BlockEncryptionStrategyLookahead;
import oram.blockenc.BlockEncryptionStrategyPath;
import oram.blockenc.BlockEncryptionStrategyTrivial;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.permutation.PermutationStrategy;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 28-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public interface Factory {
    EncryptionStrategy getEncryptionStrategy();

    CommunicationStrategy getCommunicationStrategy();

    PermutationStrategy getPermutationStrategy();

    BlockEncryptionStrategyPath getBlockEncryptionStrategyPath();

    BlockEncryptionStrategyLookahead getBlockEncryptionStrategyLookahead();

    BlockEncryptionStrategyTrivial getBlockEncryptionStrategyTrivial();
}
