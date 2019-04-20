package oram.encryption;

import javax.crypto.SecretKey;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 04-04-2019. <br>
 * Master Thesis 2019 </p>
 */

public class EncryptionStrategyCounting implements EncryptionStrategy {
    private final EncryptionStrategy encryptionStrategy;
    private int blocksEncrypted;

    public EncryptionStrategyCounting(EncryptionStrategy encryptionStrategy) {
        this.encryptionStrategy = encryptionStrategy;
        blocksEncrypted = 0;
    }

    @Override
    public SecretKey generateSecretKey(byte[] randomBytes) {
        return encryptionStrategy.generateSecretKey(randomBytes);
    }

    @Override
    public byte[] encrypt(byte[] message, SecretKey key) {
        byte[] encrypt = encryptionStrategy.encrypt(message, key);
        blocksEncrypted++;
        return encrypt;
    }

    @Override
    public byte[] decrypt(byte[] cipherText, SecretKey key) {
        return encryptionStrategy.decrypt(cipherText, key);
    }

    public int getBlocksEncrypted() {
        return blocksEncrypted;
    }

    public void resetBlocksEncrypted() {
        blocksEncrypted = 0;
    }
}
