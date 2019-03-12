package oram.clientcom;

import oram.BlockEncrypted;
import oram.Constants;
import oram.Util;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 11-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class ClientCommunicationLayer implements CommunicationStrategy {
    private final Logger logger = LogManager.getLogger("log");
    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    @Override
    public boolean start() {
        if (!setupConnection()) return false;
        return initializeStreams();
    }

    @Override
    public BlockEncrypted read(int address) {
        BlockEncrypted res;
        try {
            byte[] operationTypeBytes = Util.leIntToByteArray(0);
            int length = operationTypeBytes.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(operationTypeBytes);

            byte[] numberOfBlocks = Util.leIntToByteArray(1);
            length = numberOfBlocks.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(numberOfBlocks);

            byte[] addressBytes = Util.leIntToByteArray(address);
            length = addressBytes.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(addressBytes);

            dataOutputStream.flush();

//            byte[] receivedAddressBytes = new byte[0];
//            length = dataInputStream.readInt();
//            if (length > 0) {
//                receivedAddressBytes = new byte[length];
//                dataInputStream.readFully(receivedAddressBytes, 0, length);
//            }

            byte[] data = new byte[0];
            length = dataInputStream.readInt();
            if (length > 0) {
                data = new byte[length];
                dataInputStream.readFully(data, 0, length);
            }

            byte[] blockAddress = Arrays.copyOfRange(data, 0, Constants.ENCRYPTED_INTEGER_SIZE);
            byte[] blockData = Arrays.copyOfRange(data, Constants.ENCRYPTED_INTEGER_SIZE, length);

            res = new BlockEncrypted(blockAddress, blockData);
        } catch (IOException e) {
            logger.error("Error happened while reading block: " + e);
            logger.debug("Stacktrace", e);
            return null;
        }
        return res;
    }

    @Override
    public boolean write(int address, BlockEncrypted block) {
        try {
            byte[] operationTypeBytes = Util.leIntToByteArray(1);
            int length = operationTypeBytes.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(operationTypeBytes);

            byte[] numberOfBlocks = Util.leIntToByteArray(1);
            length = numberOfBlocks.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(numberOfBlocks);

            byte[] addressBytes = Util.leIntToByteArray(address);
            length = addressBytes.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(addressBytes);

            if (block.getAddress().length != Constants.ENCRYPTED_INTEGER_SIZE) {
                logger.error("Address byte array has wrong size: " + block.getAddress().length);
                return false;
            }

            byte[] combinedData = ArrayUtils.addAll(block.getAddress(), block.getData());
            length = combinedData.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(combinedData);

            dataOutputStream.flush();

            byte[] statusBitArray = new byte[0];
            length = dataInputStream.readInt();
            if (length > 0) {
                statusBitArray = new byte[length];
                dataInputStream.readFully(statusBitArray, 0, length);
            }
            if (Util.byteArrayToLeInt(statusBitArray) == 0)
                return false;
        } catch (IOException e) {
            logger.error("Error happened while writing block: " + e);
            logger.debug("Stacktrace", e);
            return false;
        }
        return true;
    }

    private boolean setupConnection() {
//        System.out.println("Enter IP");
//        Scanner scanner = new Scanner(System.in);
//        String hostname = scanner.nextLine();
        String hostname = "10.192.98.202"; // TODO
        try {
//            TODO: sleep and try over
            socket = new Socket(hostname, Constants.PORT);
            System.out.println("Socket opened, inet address: "+ socket.getInetAddress());
        } catch (IOException e) {
            logger.error("Error happened while initializing streams: " + e);
            logger.debug("Stacktrace", e);
            return false;
        }
        return true;
    }

    private boolean initializeStreams() {
        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            logger.error("Error happened while initializing streams: " + e);
            logger.debug("Stacktrace", e);
            return false;
        }
        return true;
    }
}
