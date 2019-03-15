package oram.server;

import oram.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 11-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class MainServer {
    private static final Logger logger = LogManager.getLogger("log");

    public static void main(String[] args) {
        logger.debug("######### INITIALIZED SERVER #########");
        logger.info("######### INITIALIZED SERVER #########");
        ServerApplication serverApplication = new ServerApplicationImpl();

        System.out.println(getIPAddress());

        ServerSocket serverSocket = openServerSocket();
        if (serverSocket == null) System.exit(-1);
        Socket socket = openSocket(serverSocket);
        if (socket == null) System.exit(-2);

        new ServerCommunicationLayer(serverApplication).run(socket);
    }

    private static ServerSocket openServerSocket() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(Constants.PORT);
        } catch (IOException e) {
            logger.error("Unable to create server socket" + e);
            logger.debug("Stacktrace", e);
            return null;
        }
        return serverSocket;
    }

    private static Socket openSocket(ServerSocket serverSocket) {
        Socket socket;
        try {
            socket = serverSocket.accept();
            System.out.println("Client accepted, inet address: " + socket.getInetAddress());
        } catch (IOException e) {
            logger.error("Unable to create socket" + e);
            logger.debug("Stacktrace", e);
            return null;
        }
        return socket;
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
