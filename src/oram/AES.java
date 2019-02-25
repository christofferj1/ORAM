package oram;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

/**
 * <p> PathORAM <br>
 * Created by Christoffer S. Jensen on 12-11-2018. <br>
 */

public class AES {
    private static final Logger logger = LogManager.getLogger("log");
    private static SecretKey secretKey;

    public static void main(String[] args) {
        byte[] cipher = AES.encrypt("1".getBytes(), "Hello World");
        byte[] message = AES.decrypt(cipher, "Hello World");

        System.out.println(new String(cipher));
        System.out.println(new String(message));
    }

    private static boolean setKeyFailed(String key) {
        try {
            byte[] keyBytes = key.getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            keyBytes = sha.digest(keyBytes);
            keyBytes = Arrays.copyOf(keyBytes, Constants.BYTES_OF_RANDOMNESS);
            secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

    public static byte[] encrypt(byte[] message, String key) {
        if (setKeyFailed(key)) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            String ivString = Util.getRandomString(Constants.BYTES_OF_RANDOMNESS);
            byte[] iv = ivString.getBytes("UTF-8");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            return ArrayUtils.addAll(iv, message);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | UnsupportedEncodingException
                | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            logger.error("Error happened while encrypting");
            logger.error(e);
            logger.debug("Stacktrace", e);
        }
        return null;
    }

    public static byte[] decrypt(byte[] cipherToDecrypt, String key) {
        if (setKeyFailed(key)) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16];
            System.arraycopy(cipherToDecrypt, 0, iv, 0, 16);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] valueCipher = new byte[Constants.BLOCK_SIZE];
            System.arraycopy(cipherToDecrypt, 16, valueCipher, 0, Constants.BLOCK_SIZE);
            byte[] decode = Base64.getDecoder().decode(valueCipher);
            return cipher.doFinal(decode);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                BadPaddingException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            logger.error("Error happened while decrypting");
            logger.error(e);
            logger.debug("Stacktrace", e);
        }
        return null;
    }
}