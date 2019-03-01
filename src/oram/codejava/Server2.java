package oram.codejava;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * This program demonstrates a simple TCP/IP socket server.
 *
 * @author www.codejava.net
 */
public class Server2 {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Enter port number");
            return;
        }

        int port = Integer.parseInt(args[0]);

        System.out.println(getIPAddress());

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server is listening on port " + port);

            byte[] message0 = new byte[]{0b1, 0b1, 0b1};
            byte[] message1 = new byte[]{0b00101010, 0b01100101, 0b01100111};

            Socket socket = serverSocket.accept();
            System.out.println("New client connected");

            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
            DataInputStream dIn = new DataInputStream(socket.getInputStream());

            while (true) {
                int length = dIn.readInt();
                byte[] message = null;
                if (length > 0) {
                    message = new byte[length];
                    dIn.readFully(message, 0, message.length); // read the message
                    System.out.println(Arrays.toString(message));
                }

                if (message != null && Arrays.equals(message, new byte[]{0b0, 0b0, 0b0, 0b0})) {
                    System.out.println("MESSAGE 0");
                    dOut.writeInt(message0.length); // write length of the message
                    dOut.write(message0);
                } else if (message != null && Arrays.equals(message, new byte[]{0b1, 0b0, 0b0, 0b0})) {
                    System.out.println("MESSAGE 1");
                    dOut.writeInt(message0.length); // write length of the message
                    dOut.write(message1);
                } else {
                    dOut.writeInt(1); // write length of the message
                    dOut.write(new byte[]{0b0});
                }

                // OutputStream output = socket.getOutputStream();
                // PrintWriter writer = new PrintWriter(output, true);

                // writer.println(new Date().toString());
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
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
}
