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
import java.util.Base64;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 05-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class AES2 {
    private static final Logger logger = LogManager.getLogger("log");
    private static SecretKey secretKey;

    public static void main(String[] args) {
        BlockPath block = new BlockPath(1337, new byte[]{0b01010, 42});
        String key = "KEY STRING";

        byte[] bytesBefore = null;

        System.out.println(block);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(block);
            bytesBefore = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        System.out.println(Arrays.toString(bytesBefore));

        byte[] cipher = AES.encrypt(bytesBefore, key);

//        System.out.println(Arrays.toString(cipher));

        byte[] res = AES.decrypt(cipher, key);

//        System.out.println(Arrays.toString(res));

        BlockPath block2 = null;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(res);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            block2 = (BlockPath) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println(block2);

//        String string = "TEST STRING 12345678";
//        byte[] bytes = string.getBytes("UTF-8");
//        byte[] cipher = AES.encrypt(bytes, "Hello World");
//        byte[] message = AES.decrypt(cipher, "Hello World");
//
//        System.out.println(new String(cipher));
//        System.out.println(new String(message));
    }

    private static boolean setKeyFailed(byte[] key) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, Constants.BYTES_OF_RANDOMNESS);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

    public static byte[] encrypt(byte[] message, byte[] key) {
        if (setKeyFailed(key)) return null;
        try {
//            if (message.length != Constants.BLOCK_SIZE) return null;
//            Initiate Cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = Util.getRandomByteArray(Constants.BYTES_OF_RANDOMNESS);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

//            Encrypt and encode
            byte[] src = cipher.doFinal(message);
            byte[] cipherByteArray = Base64.getEncoder().encode(src);

            System.out.println("IV: (size: " + iv.length + ")");
            System.out.println(Util.printByteArray(iv));
            System.out.println("Message: (size: " + message.length + ")");
            System.out.println(Util.printByteArray(message));
            System.out.println("src: (size: " + src.length + ")");
            System.out.println(Util.printByteArray(src));
            System.out.println("cipher byte array: (size: " + cipherByteArray.length + ")");
            System.out.println(Util.printByteArray(cipherByteArray));

//            Return
            return ArrayUtils.addAll(iv, src);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
            logger.error("Error happened while encrypting");
            logger.error(e);
            logger.debug("Stacktrace", e);
        }
        return null;
    }

    public static byte[] decrypt(byte[] cipherToDecrypt, byte[] key) {
        if (setKeyFailed(key)) return null;
        try {
//            Initiate Cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[Constants.BYTES_OF_RANDOMNESS];
            System.arraycopy(cipherToDecrypt, 0, iv, 0, Constants.BYTES_OF_RANDOMNESS);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            int cipherLength = cipherToDecrypt.length - Constants.BLOCK_SIZE;
            byte[] valueCipher = new byte[cipherLength];
            System.arraycopy(cipherToDecrypt, Constants.BYTES_OF_RANDOMNESS, valueCipher, 0, cipherLength);

//            Decode and decrypt
//            byte[] decode = Base64.getDecoder().decode(valueCipher);
            byte[] message = cipher.doFinal(valueCipher);

            System.out.println("IV: (size: " + iv.length + ")");
            System.out.println(Util.printByteArray(iv));
            System.out.println("Cipher to decrypt: (size: " + cipherToDecrypt.length + ")");
            System.out.println(Util.printByteArray(cipherToDecrypt));
            System.out.println("Value cipher: (size: " + valueCipher.length + ")");
            System.out.println(Util.printByteArray(valueCipher));
//            System.out.println("Decode: (size: " + decode.length + ")");
//            System.out.println(Util.printByteArray(decode));

            return message;
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
