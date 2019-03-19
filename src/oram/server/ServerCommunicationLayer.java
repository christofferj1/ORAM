package oram.server;

import oram.OperationType;
import oram.Util;
import oram.block.BlockStandard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 11-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class ServerCommunicationLayer {
    private final Logger logger = LogManager.getLogger("log");
    private Socket socket;
    private ServerApplication application;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    public ServerCommunicationLayer(ServerApplication application) {
        this.application = application;
    }

    void run(Socket socket) {
        this.socket = socket;

        if (!initializeStreams())
            System.exit(-2);

        while (true) {
            AccessEvent accessEvent = receiveRequests();
            if (accessEvent == null || accessEvent.getOperationType() == null) break;

            List<String> addresses = accessEvent.getAddresses();
            System.out.println("Received access event of type: " + accessEvent.getOperationType() +
                    ", to addresses: " + Arrays.toString(addresses.toArray()));

            if (accessEvent.getOperationType().equals(OperationType.READ)) { // Handle a read event
                if (!sendBlocks(application.read(addresses))) break;
            } else { // Handle a write event
                List<byte[]> dataArrays = accessEvent.getDataArrays();
                boolean statusBit = application.write(addresses, dataArrays);
                boolean sendStatusBit = sendWritingStatusBit(statusBit); // TODO: test the workflow if status = 0

                if (!(statusBit && sendStatusBit)) break;
            }
//            logger.info("Handled a: " + accessEvent.getOperationType() + " event, addresses: " + Arrays.toString(addresses.toArray()));
        }
//        TODO: close session
    }

    private boolean initializeStreams() {
        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            logger.error("Error happened while initializing streams: " + e);
            logger.debug("Stacktrace", e);
        }
        return true;
    }

    private AccessEvent receiveRequests() {
        byte[] operationTypeBytes = readBytes();
        if (operationTypeBytes == null) return null;
        byte[] numberOfRequestsBytes = readBytes();
        if (numberOfRequestsBytes == null) return null;

        int operationTypeInt = Util.byteArrayToLeInt(operationTypeBytes);
        OperationType op = operationTypeInt == 0 ? OperationType.READ : OperationType.WRITE;

        int numberOfRequests = Util.byteArrayToLeInt(numberOfRequestsBytes);

        if (op.equals(OperationType.READ)) {
            List<String> addresses = new ArrayList<>();
            for (int i = 0; i < numberOfRequests; i++) {
                byte[] addressBytes = readBytes();
                if (addressBytes == null) return null;
                addresses.add(Integer.toString(Util.byteArrayToLeInt(addressBytes)));
            }
            return new AccessEvent(addresses, null, op);
        } else {
            List<String> addresses = new ArrayList<>();
            List<byte[]> dataArrays = new ArrayList<>();
            for (int i = 0; i < numberOfRequests; i++) {
                byte[] addressBytes = readBytes();
                if (addressBytes == null) return null;
                addresses.add(Integer.toString(Util.byteArrayToLeInt(addressBytes)));

                byte[] data = readBytes();
                if (data == null) return null;
                dataArrays.add(data);
            }
            return new AccessEvent(addresses, dataArrays, op);
        }
    }

    private boolean sendBlocks(List<BlockStandard> blocks) {
        try {
            for (BlockStandard block : blocks) {
                System.out.println("Sending block: " + block.toStringShort());
                int length = block.getData().length;
                dataOutputStream.write(Util.beIntToByteArray(length));
                dataOutputStream.write(block.getData());
            }
            dataOutputStream.flush();
        } catch (IOException e) {
            logger.error("Error happened while sending block: " + e);
            logger.debug("Stacktrace", e);
            return false;
        }
        return true;
    }

    private boolean sendWritingStatusBit(boolean status) {
        try {
            System.out.println("Sending writing status: " + status);
            byte[] bytes = Util.leIntToByteArray(status ? 1 : 0);
            int length = bytes.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(bytes);

            dataOutputStream.flush();
        } catch (IOException e) {
            logger.error("Error happened while sending status bit: " + e);
            logger.debug("Stacktrace", e);
            return false;
        }
        return true;
    }

    private byte[] readBytes() {
        byte[] res = null;
        try {
            int length = dataInputStream.readInt();
            if (length > 0) {
                res = new byte[length];
                dataInputStream.readFully(res, 0, length);
            }
        } catch (IOException e) {
            logger.error("Error happened while receiving requests: " + e);
            logger.debug("Stacktrace", e);
            return null;
        }
        return res;
    }
}
