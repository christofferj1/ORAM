package oram;

import oram.path.BlockPath;

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
 * Created by Christoffer S. Jensen on 05-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class AES2 {
    private static SecretKey secretKey;

    public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        BlockPath block = new BlockPath(1337, new byte[]{0b01010, 42});
        String key = "KEY STRING";
        setKeyFailed(key);

        byte[] bytesBefore = null;

        System.out.println(block);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(block);
            bytesBefore = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Cipher cipherEncrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = Util.getRandomByteArray(Constants.BYTES_OF_RANDOMNESS);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipherEncrypt.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

//            Encrypt and encode
        byte[] src = cipherEncrypt.doFinal(bytesBefore);
//        byte[] cipherByteArray = Base64.getEncoder().encode(src);


        System.out.println("IV: (size: " + iv.length + ")");
        System.out.println(Util.printByteArray(iv));
        System.out.println("Message: (size: " + bytesBefore.length + ")");
        System.out.println(Util.printByteArray(bytesBefore));
        System.out.println("src: (size: " + src.length + ")");
        System.out.println(Util.printByteArray(src));
//        System.out.println("cipher byte array: (size: " + cipherByteArray.length + ")");
//        System.out.println(Util.printByteArray(cipherByteArray));



        Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
//        System.arraycopy(cipherByteArray, 0, iv, 0, Constants.BYTES_OF_RANDOMNESS);
        cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
//        int cipherLength = cipherByteArray.length - Constants.BLOCK_SIZE;
//        byte[] valueCipher = new byte[cipherLength];
//        System.arraycopy(cipherByteArray, Constants.BYTES_OF_RANDOMNESS, valueCipher, 0, cipherLength);


//        System.out.println("Value cipher: (size: " + cipherByteArray.length + ")");
//        System.out.println(Util.printByteArray(cipherByteArray));

//            Decode and decrypt
//        byte[] decode = Base64.getDecoder().decode(cipherByteArray);
        byte[] message = cipherDecrypt.doFinal(src);

        BlockPath block2 = null;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(message);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            block2 = (BlockPath) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println(block2);
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
}
