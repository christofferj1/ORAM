import oram.Constants;
import oram.Util;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 05-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class TestServer {
    private static ServerSocket serverSocket;
    private static Socket socket;
    private static DataOutputStream dataOutputStream;
    private static DataInputStream dataInputStream;


    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Enter port number");
            return;
        }

        int port = Integer.parseInt(args[0]);
        System.out.println(getIPAddress());

        if (!openSocket(port))
            System.exit(-1);

        if (!initializeStreams())
            System.exit(-2);

        Pair<byte[], byte[]> pair = receiveBlock();
        if (pair == null)
            System.exit(-3);

        byte[] bytesForFile = ArrayUtils.addAll(pair.getKey(), pair.getValue());

        if (!writeFile(bytesForFile))
            System.exit(-4);

        byte[] bytesFromFile = readFile();
        if (bytesForFile == null)
            System.exit(-5);

        byte[] address = new byte[32];
        byte[] data = new byte[Constants.BYTES_OF_RANDOMNESS * 8 + 16];
        System.arraycopy(bytesFromFile, 0, address, 0, 32);
        System.arraycopy(bytesFromFile, 32, data, 0, Constants.BYTES_OF_RANDOMNESS * 8 + 16);

        if (!sendBlock(data, address))
            System.exit(-6);

        if (!closeSocket())
            System.exit(-7);
    }

    private static String getIPAddress() {
        Enumeration en = null;
        try {
            en = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while (en.hasMoreElements()) {
            NetworkInterface i = (NetworkInterface) en.nextElement();
            for (Enumeration en2 = i.getInetAddresses(); en2.hasMoreElements(); ) {
                InetAddress addr = (InetAddress) en2.nextElement();
                if (!addr.isLoopbackAddress()) {
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        }
        return null;
    }

    private static boolean openSocket(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Socket opened");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean initializeStreams() {
        try {
            socket = serverSocket.accept();
            System.out.println("Client accepted");

            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("Opened output stream");
            dataInputStream = new DataInputStream(socket.getInputStream());
            System.out.println("Opened input stream");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static Pair<byte[], byte[]> receiveBlock() {
        System.out.println("Ready to receive blocks");
        byte[] message0 = new byte[0];
        byte[] message1 = new byte[0];
        try {
            int length = dataInputStream.readInt();
            System.out.println("Received first length: " + length);
            if (length > 0) {
                message0 = new byte[length];
                dataInputStream.readFully(message0, 0, message0.length);
                System.out.println("Received address array: " + Arrays.toString(message0) + ", of length: " + length);
            }

            length = dataInputStream.readInt();
            System.out.println("Received second length: " + length);
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

    private static boolean writeFile(byte[] bytesForFile) {
        try (FileOutputStream fos = new FileOutputStream("test_file")) {
            fos.write(bytesForFile);
            System.out.println("File written");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static byte[] readFile() {
        byte[] res;
        try {
            res = Files.readAllBytes(Paths.get("test_file"));
            System.out.println("File read");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return res;
    }

    private static boolean sendBlock(byte[] data, byte[] address) {
        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

            int length = address.length;
            byte[] bytes = Util.beIntToByteArray(length);
            dataOutputStream.write(bytes);
            dataOutputStream.write(address);
            System.out.println("Address send: (size: " + length + ")\n" + Arrays.toString(address));
            int length1 = data.length;
            byte[] bytes1 = Util.beIntToByteArray(length1);
            dataOutputStream.write(bytes1);
            dataOutputStream.write(data);
            System.out.println("Data send: (size: " + length1 + ")\n" + Arrays.toString(data));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean closeSocket() {
        try {
            if (serverSocket != null)
                serverSocket.close();
            System.out.println("Socket closed");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}