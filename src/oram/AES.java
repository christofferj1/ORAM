package oram;

import oram.path.BlockPath;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 20-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class AES {
    private static final Logger logger = LogManager.getLogger("log");
    private static SecretKey secretKey;
//    TODO: optimize by create one key on the client and pass it around
//    TODO: test if it is faster to do non statically

    public static void main(String[] args) {
        BlockPath block = new BlockPath(1337, new byte[]{0b01010, 42});

        byte[] bytesBefore = null;
        System.out.println(block);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(block);
            bytesBefore = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] key = "KEY STRING".getBytes();
        byte[] cipher = AES.encrypt(bytesBefore, key);

        byte[] res = AES.decrypt(cipher, key);

        BlockPath block2 = null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(res);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            block2 = (BlockPath) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println(block2);
        System.out.println(block.equals(block2));
    }

    private static boolean setKeyFailed(byte[] key) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, Constants.BYTES_OF_RANDOMNESS);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error happened generating a key");
            logger.error(e);
            logger.debug("Stacktrace", e);
            return true;
        }
        return false;
    }

    public static byte[] encrypt(byte[] message, byte[] key) {
        if (setKeyFailed(key)) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = Util.getRandomByteArray(Constants.BYTES_OF_RANDOMNESS);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

            return ArrayUtils.addAll(iv, cipher.doFinal(message));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                BadPaddingException | InvalidAlgorithmParameterException e) {
            logger.error("Error happened while encrypting");
            logger.error(e);
            logger.debug("Stacktrace", e);
        }
        return null;
    }

    public static byte[] decrypt(byte[] ciphertext, byte[] key) {
        if (setKeyFailed(key)) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = Arrays.copyOf(ciphertext, Constants.AES_BLOCK_BYTES);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] valueCipher = Arrays.copyOfRange(ciphertext, Constants.AES_BLOCK_BYTES, ciphertext.length);

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
