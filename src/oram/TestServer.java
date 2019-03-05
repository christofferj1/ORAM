package oram;

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

        byte[] address = new byte[8];
        byte[] data = new byte[Constants.BYTES_OF_RANDOMNESS * 8];
        System.arraycopy(bytesFromFile, 0, address, 0, 8);
        System.arraycopy(bytesFromFile, 8, data, 0, Constants.BYTES_OF_RANDOMNESS * 8);

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
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean initializeStreams() {
        try {
            socket = serverSocket.accept();

            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
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
                System.out.println("Received byte array: " + Arrays.toString(message0) + ", of length: " + length);
            }

            length = dataInputStream.readInt();
            if (length > 0) {
                message1 = new byte[length];
                dataInputStream.readFully(message1, 0, message1.length);
                System.out.println("Received byte array: " + Arrays.toString(message1) + ", of length: " + length);
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

            dataOutputStream.write(address.length);
            dataOutputStream.write(address);
            dataOutputStream.write(data.length);
            dataOutputStream.write(data);
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
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}