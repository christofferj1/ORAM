package oram.encryption;

import javax.crypto.SecretKey;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 04-04-2019. <br>
 * Master Thesis 2019 </p>
 */

public class EncryptionStrategyTiming implements EncryptionStrategy {
    private final EncryptionStrategy encryptionStrategy;
    private long time;

    public EncryptionStrategyTiming(EncryptionStrategy encryptionStrategy) {
        this.encryptionStrategy = encryptionStrategy;
        time = 0;
    }

    @Override
    public SecretKey generateSecretKey(byte[] randomBytes) {
        long startTime = System.nanoTime();
        SecretKey secretKey = encryptionStrategy.generateSecretKey(randomBytes);
        time += System.nanoTime() - startTime;
        return secretKey;
    }

    @Override
    public byte[] encrypt(byte[] message, SecretKey key) {
        long startTime = System.nanoTime();
        byte[] encrypt = encryptionStrategy.encrypt(message, key);
        time += System.nanoTime() - startTime;
        return encrypt;
    }

    @Override
    public byte[] decrypt(byte[] cipherText, SecretKey key) {
        long startTime = System.nanoTime();
        byte[] decrypt = encryptionStrategy.decrypt(cipherText, key);
        time += System.nanoTime() - startTime;
        return decrypt;
    }

    public long getTime() {
        return time;
    }
}
