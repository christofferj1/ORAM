package oram.codejava;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Scanner;

/**
 * This program demonstrates a simple TCP/IP socket client.
 *
 * @author www.codejava.net
 */
public class Client2 {

    public static void main(String[] args) {
        if (args.length < 2) return;

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        Scanner scanner = new Scanner(System.in);

        try (Socket socket = new Socket(hostname, port)) {
            // InputStream input = socket.getInputStream();
            // BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
            DataInputStream dIn = new DataInputStream(socket.getInputStream());

            while (true) {
                String intString = scanner.nextLine();
                Integer i = Integer.parseInt(intString);
                System.out.println("Your number is " + i);
                byte[] arr = leIntToByteArray(i);
                // System.out.println("In bytes it is " + Arrays.toString(leIntToByteArray(i)));

                dOut.writeInt(arr.length);
                dOut.write(arr);

                int length = dIn.readInt();                    // read length of incoming message
                if(length>0) {
                    byte[] message = new byte[length];
                    dIn.readFully(message, 0, message.length); // read the message
                    System.out.println("Received byte array: " + Arrays.toString(message) + ", of length: " + length);
                }

                // String time = reader.readLine();

                // System.out.println(time);

            }
        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    private static byte[] leIntToByteArray(int i) {
        final ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(i);
        return bb.array();
    }
}
