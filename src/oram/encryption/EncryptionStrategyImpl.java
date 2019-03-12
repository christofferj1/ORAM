package oram.encryption;

import oram.Constants;
import oram.Util;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 12-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class EncryptionStrategyImpl implements EncryptionStrategy {
    private final Logger logger = LogManager.getLogger("log");

    @Override
    public SecretKey generateSecretKey(byte[] randomBytes) {
        SecretKey res;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            randomBytes = sha.digest(randomBytes);
            randomBytes = Arrays.copyOf(randomBytes, Constants.BYTES_OF_RANDOMNESS);
            res = new SecretKeySpec(randomBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error happened generating a key");
            logger.error(e);
            logger.debug("Stacktrace", e);
            return null;
        }
        return res;
    }

    @Override
    public byte[] encrypt(byte[] message, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = Util.getRandomByteArray(Constants.BYTES_OF_RANDOMNESS);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

            return ArrayUtils.addAll(iv, cipher.doFinal(message));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                BadPaddingException | InvalidAlgorithmParameterException e) {
            logger.error("Error happened while encrypting");
            logger.error(e);
            logger.debug("Stacktrace", e);
        }
        return null;
    }

    @Override
    public byte[] decrypt(byte[] cipherText, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = Arrays.copyOf(cipherText, Constants.AES_BLOCK_BYTES);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] valueCipher = Arrays.copyOfRange(cipherText, Constants.AES_BLOCK_BYTES, cipherText.length);

            return cipher.doFinal(valueCipher);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                BadPaddingException | InvalidAlgorithmParameterException e) {
            logger.error("Error happened while decrypting");
            logger.error(e);
            logger.debug("Stacktrace", e);
        }
        return null;

    }
}
