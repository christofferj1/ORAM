package oram;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 07-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class AESTest {
    @Test
    public void shouldBeAbleToEncryptAndDecrypt() {
        String message = "Some message";
        byte[] key = Util.getRandomByteArray(16);
        byte[] ciphertext = AES.encrypt(message.getBytes(), key);
        byte[] plaintext = AES.decrypt(ciphertext, key);
        assertNotNull(plaintext);
        assertThat(new String(plaintext), is(message));
    }

    @Test
    public void shouldHandleDifferentKeySizes() {
        String message = "Some message";

//        Key size = 1 byte
        byte[] key = Util.getRandomByteArray(1);
        byte[] ciphertext = AES.encrypt(message.getBytes(), key);
        byte[] plaintext = AES.decrypt(ciphertext, key);
        assertNotNull(plaintext);
        assertThat(new String(plaintext), is(message));

//        Key size = 100 bytes
        key = Util.getRandomByteArray(100);
        ciphertext = AES.encrypt(message.getBytes(), key);
        plaintext = AES.decrypt(ciphertext, key);
        assertNotNull(plaintext);
        assertThat(new String(plaintext), is(message));
    }

    @Test
    public void shouldHandleMessagesOfDifferentSizes() {
        byte[] key = Util.getRandomByteArray(16);

//        Message size = 1 character
        String message = "a";
        byte[] ciphertext = AES.encrypt(message.getBytes(), key);
        byte[] plaintext = AES.decrypt(ciphertext, key);
        assertNotNull(plaintext);
        assertThat(new String(plaintext), is(message));

//        Message size = 445 characters
        message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        ciphertext = AES.encrypt(message.getBytes(), key);
        plaintext = AES.decrypt(ciphertext, key);
        assertNotNull(plaintext);
        assertThat(new String(plaintext), is(message));
    }
}
