package oram.encryption;

import javax.crypto.SecretKey;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 12-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public interface EncryptionStrategy {
    SecretKey generateSecretKey(byte[] randomBytes);

    byte[] encrypt(byte[] message, SecretKey key);

    byte[] decrypt(byte[] cipherText, SecretKey key);
}
