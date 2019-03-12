package oram.encryption;

import oram.Util;
import org.junit.Test;

import javax.crypto.SecretKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 07-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class EncryptionStrategyTest {
    @Test
    public void shouldBeAbleToEncryptAndDecrypt() {
        byte[] key = Util.getRandomByteArray(16);
        EncryptionStrategy encryptionStrategy = new EncryptionStrategyImpl();
        SecretKey secretKey = encryptionStrategy.generateSecretKey(key);

        String message = "Some message";
        byte[] ciphertext = encryptionStrategy.encrypt(message.getBytes(), secretKey);
        byte[] plaintext = encryptionStrategy.decrypt(ciphertext, secretKey);
        assertNotNull(plaintext);
        assertThat(new String(plaintext), is(message));
    }

    @Test
    public void shouldHandleDifferentKeySizes() {
        String message = "Some message";
        byte[] key = Util.getRandomByteArray(1);
        EncryptionStrategy encryptionStrategy = new EncryptionStrategyImpl();
        SecretKey secretKey = encryptionStrategy.generateSecretKey(key);

//        Key size = 1 byte
        byte[] ciphertext = encryptionStrategy.encrypt(message.getBytes(), secretKey);
        byte[] plaintext = encryptionStrategy.decrypt(ciphertext, secretKey);
        assertNotNull(plaintext);
        assertThat(new String(plaintext), is(message));

//        Key size = 100 bytes
        key = Util.getRandomByteArray(100);
        encryptionStrategy = new EncryptionStrategyImpl();
        secretKey = encryptionStrategy.generateSecretKey(key);

        ciphertext = encryptionStrategy.encrypt(message.getBytes(), secretKey);
        plaintext = encryptionStrategy.decrypt(ciphertext, secretKey);
        assertNotNull(plaintext);
        assertThat(new String(plaintext), is(message));
    }

    @Test
    public void shouldHandleMessagesOfDifferentSizes() {
        byte[] key = Util.getRandomByteArray(16);
        EncryptionStrategy encryptionStrategy = new EncryptionStrategyImpl();
        SecretKey secretKey = encryptionStrategy.generateSecretKey(key);

//        Message size = 1 character
        String message = "a";
        byte[] ciphertext = encryptionStrategy.encrypt(message.getBytes(), secretKey);
        byte[] plaintext = encryptionStrategy.decrypt(ciphertext, secretKey);
        assertNotNull(plaintext);
        assertThat(new String(plaintext), is(message));

//        Message size = 445 characters
        message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        ciphertext = encryptionStrategy.encrypt(message.getBytes(), secretKey);
        plaintext = encryptionStrategy.decrypt(ciphertext, secretKey);
        assertNotNull(plaintext);
        assertThat(new String(plaintext), is(message));
    }
}
