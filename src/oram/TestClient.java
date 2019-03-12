package oram;

import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyImpl;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 05-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class TestClient {
    private static Socket socket;
    private static DataOutputStream dataOutputStream;
    private static DataInputStream dataInputStream;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Enter port number");
            return;
        }

        if (!openSocket(args))
            System.exit(-1);

        byte[] data = Util.getRandomByteArray(Constants.BYTES_OF_RANDOMNESS * 8 - 1);
        byte[] address = Util.leIntToByteArray(1337);
        BlockEncrypted block = new BlockEncrypted(address, data);
        System.out.println(block);

        byte[] key = Util.getRandomByteArray(Constants.BYTES_OF_RANDOMNESS);

        EncryptionStrategy encryptionStrategy = new EncryptionStrategyImpl();
        SecretKey secretKey = encryptionStrategy.generateSecretKey(key);
        byte[] addressCipher = encryptionStrategy.encrypt(address, secretKey);
        byte[] dataCipher = encryptionStrategy.encrypt(data, secretKey);

        System.out.println("Address cipher: (of length: " + addressCipher.length + ")\n" + Arrays.toString(addressCipher));
        System.out.println("Address cipher: (of length: " + dataCipher.length + ")\n" + Arrays.toString(dataCipher));

        if (addressCipher == null || dataCipher == null || !sendBlock(dataCipher, addressCipher))
            System.exit(-2);

        Pair<byte[], byte[]> pair = receiveBlock();
        if (pair == null)
            System.exit(-3);

        byte[] add = pair.getKey();
        byte[] addressReceived = encryptionStrategy.decrypt(add, secretKey);
        byte[] dat = pair.getValue();
        byte[] dataReceived = encryptionStrategy.decrypt(dat, secretKey);

        BlockEncrypted block2 = new BlockEncrypted(addressReceived, dataReceived);
        System.out.println(block2);

        System.out.println(block.equals(block2));

        if (!closeSocket())
            System.exit(-4);
    }

    private static boolean createBlockFile() {
        int address = 1337;
        byte[] data = Util.getRandomByteArray(Constants.BYTES_OF_RANDOMNESS * 8);
        byte[] res = new byte[4 + data.length];

        try (FileOutputStream fos = new FileOutputStream("pathname")) {
            fos.write(res);
            //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean sendBlock(byte[] data, byte[] address) {
        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

            int length = address.length;
            byte[] bytes = Util.beIntToByteArray(length);
            dataOutputStream.write(bytes);
            dataOutputStream.write(address);
            System.out.println("Address send");
            dataOutputStream.write(Util.beIntToByteArray(data.length));
            dataOutputStream.write(data);
            System.out.println("Data send");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static Pair<byte[], byte[]> receiveBlock() {
        byte[] message0 = new byte[0];
        byte[] message1 = new byte[0];
        try {
            int length = dataInputStream.readInt();
            if (length > 0) {
                message0 = new byte[length];
                dataInputStream.readFully(message0, 0, message0.length);
                System.out.println("Received address array: " + Arrays.toString(message0) + ", of length: " + length);
            }

            length = dataInputStream.readInt();
            if (length > 0) {
                message1 = new byte[length];
                dataInputStream.readFully(message1, 0, message1.length);
                System.out.println("Received data array: " + Arrays.toString(message1) + ", of length: " + length);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return new MutablePair<>(message0, message1);
    }

    private static boolean openSocket(String[] args) {
        int port = Integer.parseInt(args[0]);

        System.out.println("Enter IP");
        Scanner scanner = new Scanner(System.in);
        String hostname = scanner.nextLine();

        try {
            socket = new Socket(hostname, port);
            System.out.println("Socket opened");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean closeSocket() {
        try {
            if (dataOutputStream != null)
                dataOutputStream.close();
            System.out.println("Socket closed");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
