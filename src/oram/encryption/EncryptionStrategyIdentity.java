package oram.encryption;

import oram.Constants;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 12-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class EncryptionStrategyIdentity implements EncryptionStrategy {
    @Override
    public SecretKey generateSecretKey(byte[] randomBytes) {
        SecretKey res;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            randomBytes = sha.digest(randomBytes);
            randomBytes = Arrays.copyOf(randomBytes, Constants.BYTES_OF_RANDOMNESS);
            res = new SecretKeySpec(randomBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        return res;
    }

    @Override
    public byte[] encrypt(byte[] message, SecretKey key) {
        return message;
    }

    @Override
    public byte[] decrypt(byte[] cipherText, SecretKey key) {
        return cipherText;
    }
}
