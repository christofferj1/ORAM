package oram.factory;

import oram.CommunicationStrategyStub;
import oram.clientcom.CommunicationStrategy;
import oram.clientcom.CommunicationStrategyImpl;
import oram.clientcom.CommunicationStrategyTiming;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyIdentity;
import oram.encryption.EncryptionStrategyImpl;
import oram.encryption.EncryptionStrategyTiming;
import oram.permutation.PermutationStrategy;
import oram.permutation.PermutationStrategyIdentity;
import oram.permutation.PermutationStrategyImpl;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 15-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class FactoryCustom implements Factory {
    private final Enc enc;
    private final Com com;
    private final Per per;
    private EncryptionStrategy encryptionStrategy;
    private EncryptionStrategyTiming encryptionStrategyTiming;
    private CommunicationStrategy communicationStrategy;
    private CommunicationStrategyTiming communicationStrategyTiming;
    private PermutationStrategy permutationStrategy;
    private int size;
    private int bucketSize;

    public FactoryCustom(Enc enc, Com com, Per per, int size, int bucketSize) {
        this.enc = enc;
        this.com = com;
        this.per = per;
        this.size = size;
        this.bucketSize = bucketSize;
    }

    @Override
    public EncryptionStrategy getEncryptionStrategy() {
        if (encryptionStrategy == null) {
            switch (enc) {
                case IDEN:
                    encryptionStrategy = new EncryptionStrategyIdentity();
                    break;
                case IMPL:
                    encryptionStrategy = new EncryptionStrategyImpl();
            }
            encryptionStrategyTiming = new EncryptionStrategyTiming(encryptionStrategy);
        }
        return encryptionStrategyTiming;
    }

    @Override
    public CommunicationStrategy getCommunicationStrategy() {
        if (communicationStrategy == null) {
            switch (com) {
                case IMPL:
                    communicationStrategy = new CommunicationStrategyImpl();
                    break;
                case STUB:
                    communicationStrategy = new CommunicationStrategyStub(size, bucketSize);
            }
            communicationStrategyTiming = new CommunicationStrategyTiming(communicationStrategy);
        }
        return communicationStrategyTiming;
    }

    @Override
    public PermutationStrategy getPermutationStrategy() {
        if (permutationStrategy == null) {
            switch (per) {
                case IMPL:
                    permutationStrategy = new PermutationStrategyImpl();
                    break;
                case IDEN:
                    permutationStrategy = new PermutationStrategyIdentity();
            }
        }
        return permutationStrategy;
    }

    public enum Enc {
        IDEN, IMPL
    }

    public enum Com {
        STUB, IMPL
    }

    public enum Per {
        IDEN, IMPL
    }
}