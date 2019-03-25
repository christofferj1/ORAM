import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Scanner;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 25-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class Server_Client {


    public static void main(String[] args) {
        System.out.println("Acting as server? [y/n]");
        Scanner scanner = new Scanner(System.in);
        String serverClient = scanner.nextLine();
        while (!(serverClient.toLowerCase().equals("y") || serverClient.toLowerCase().equals("n"))) {
            System.out.println("Answer 'y' or 'n'");
            serverClient = scanner.nextLine();
        }
        if (serverClient.toLowerCase().equals("y"))
            runAsServer();
        else
            runAsClient();
    }

    private static void runAsServer() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter port number");
        String portNumberString = scanner.nextLine();
        while (!portNumberString.matches("\\d+")) {
            System.out.println("Enter port as an integer");
            portNumberString = scanner.nextLine();
        }
        int portNumber = Integer.parseInt(portNumberString);

        System.out.println(getIPAddress());

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {

            System.out.println("Server is listening on port " + portNumberString);

            Socket socket = serverSocket.accept();
            System.out.println("New client connected");

            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
            DataInputStream dIn = new DataInputStream(socket.getInputStream());

            while (true) {
                System.out.println("Send message:");
                String message = scanner.nextLine();

                dOut.writeInt(message.getBytes().length);
                dOut.write(message.getBytes());

                dOut.flush();

                int length = dIn.readInt();
                if (length > 0) {
                    byte[] response = new byte[length];
                    dIn.readFully(response, 0, response.length); // read the message
                    System.out.println("Received response");
                    System.out.println("    Length: " + length);
                    System.out.println("    Array: " + Arrays.toString(response));
                    System.out.println("    String: " + new String(response));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void runAsClient() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter port number");
        String portNumberString = scanner.nextLine();
        while (!portNumberString.matches("\\d+")) {
            System.out.println("Enter port as an integer");
            portNumberString = scanner.nextLine();
        }
        int portNumber = Integer.parseInt(portNumberString);

        System.out.println("Enter IP");
        String ipAddress = scanner.nextLine();
        while (!ipAddress.matches("\\d+.\\d+.\\d+.\\d+")) {
            System.out.println("Enter ip ass d.d.d.d");
            ipAddress = scanner.nextLine();
        }

        try (Socket socket = new Socket(ipAddress, portNumber)) {
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
            DataInputStream dIn = new DataInputStream(socket.getInputStream());

            while (true) {
                int length = dIn.readInt();
                if (length > 0) {
                    byte[] message = new byte[length];
                    dIn.readFully(message, 0, message.length); // read the message
                    System.out.println("Received response");
                    System.out.println("    Length: " + length);
                    System.out.println("    Array: " + Arrays.toString(message));
                    System.out.println("    String: " + new String(message));
                }

                System.out.println("Send response:");
                String response = scanner.nextLine();

                dOut.writeInt(response.getBytes().length);
                dOut.write(response.getBytes());

                dOut.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
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
