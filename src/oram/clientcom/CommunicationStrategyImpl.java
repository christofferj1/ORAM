package oram.clientcom;

import oram.Constants;
import oram.Util;
import oram.block.BlockEncrypted;
import org.apache.commons.lang3.ArrayUtils;
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

public class CommunicationStrategyImpl implements CommunicationStrategy {
    private final Logger logger = LogManager.getLogger("log");
    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    @Override
    public boolean start(String ipAddress) {
        if (!setupConnection(ipAddress)) return false;
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
            if (Util.byteArrayToLeInt(statusBitArray) == 0) {
                logger.error("Status bit received was 0");
                return false;
            }
        } catch (IOException e) {
            logger.error("Error happened while writing block: " + e);
            logger.debug("Stacktrace", e);
            return false;
        }
        return true;
    }

    @Override
    public List<BlockEncrypted> readArray(List<Integer> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            logger.error("Cannot read empty array of addresses");
            return null;
        }

        int addressSize = addresses.size();
        List<BlockEncrypted> res = new ArrayList<>();
        try {
//            Send operation type
            byte[] operationTypeBytes = Util.leIntToByteArray(0);
            int length = operationTypeBytes.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(operationTypeBytes);

//            Send number of blocks
            byte[] numberOfBlocks = Util.leIntToByteArray(addressSize);
            length = numberOfBlocks.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(numberOfBlocks);

//            Send addresses
            byte[] addressBytes;
            for (int i : addresses) {
                addressBytes = Util.leIntToByteArray(i);
                length = addressBytes.length;
                dataOutputStream.write(Util.beIntToByteArray(length));
                dataOutputStream.write(addressBytes);
            }
            dataOutputStream.flush();

            for (int i = 0; i < addressSize; i++) {
                byte[] data = new byte[0];
                length = dataInputStream.readInt();
                if (length > 0) {
                    data = new byte[length];
                    dataInputStream.readFully(data, 0, length);
                }

                byte[] blockAddress = Arrays.copyOfRange(data, 0, Constants.ENCRYPTED_INTEGER_SIZE);
                byte[] blockData = Arrays.copyOfRange(data, Constants.ENCRYPTED_INTEGER_SIZE, length);

                res.add(new BlockEncrypted(blockAddress, blockData));
            }
        } catch (IOException e) {
            logger.error("Error happened while reading block: " + e);
            logger.debug("Stacktrace", e);
            return null;
        }
        return res;

    }

    @Override
    public boolean writeArray(List<Integer> addresses, List<BlockEncrypted> blocks) {
        boolean res = true;
        try {
//            Send operation type
            byte[] operationTypeBytes = Util.leIntToByteArray(1);
            int length = operationTypeBytes.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(operationTypeBytes);

//            Send number of blocks
            byte[] numberOfBlocks = Util.leIntToByteArray(blocks.size());
            length = numberOfBlocks.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(numberOfBlocks);

//            Send blocks
            for (int i = 0; i < blocks.size(); i++) {
                byte[] addressBytes = Util.leIntToByteArray(addresses.get(i));
                length = addressBytes.length;
                dataOutputStream.write(Util.beIntToByteArray(length));
                dataOutputStream.write(addressBytes);

                if (blocks.get(i).getAddress().length != Constants.ENCRYPTED_INTEGER_SIZE) {
                    logger.error("Address byte array has wrong size: " + blocks.get(i).getAddress().length);
                    res = false;
                    break;
                }

                byte[] combinedData = ArrayUtils.addAll(blocks.get(i).getAddress(), blocks.get(i).getData());
                length = combinedData.length;
                dataOutputStream.write(Util.beIntToByteArray(length));
                dataOutputStream.write(combinedData);
            }

            byte[] statusBitArray = new byte[0];
            if (res) {
                dataOutputStream.flush();

                statusBitArray = new byte[0];
                length = dataInputStream.readInt();
                if (length > 0) {
                    statusBitArray = new byte[length];
                    dataInputStream.readFully(statusBitArray, 0, length);
                }
            }
            if (Util.byteArrayToLeInt(statusBitArray) == 0) {
                logger.error("Status bit received was 0");
                res = false;
            }
        } catch (IOException e) {
            logger.error("Error happened while writing block: " + e);
            logger.debug("Stacktrace", e);
            res = false;
        }
        return res;
    }

    @Override
    public boolean sendEndSignal() {
        try {
//            Send operation type
            byte[] operationTypeBytes = Util.leIntToByteArray(2);
            int length = operationTypeBytes.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(operationTypeBytes);

//            Send number of blocks, just to follow the protocol
            byte[] numberOfBlocks = Util.leIntToByteArray(0);
            length = numberOfBlocks.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(numberOfBlocks);

            dataOutputStream.flush();

            byte[] statusBitArray = new byte[0];
            length = dataInputStream.readInt();
            if (length > 0) {
                statusBitArray = new byte[length];
                dataInputStream.readFully(statusBitArray, 0, length);
            }

            if (Util.byteArrayToLeInt(statusBitArray) == 0) {
                logger.error("Status bit received (when overwriting blocks) was 0");
                return false;
            }
        } catch (IOException e) {
            logger.error("Error happened while reading block: " + e);
            logger.debug("Stacktrace", e);
            return false;
        }
        return true;
    }

    @Override
    public long speedTest() {
        long startTime = System.nanoTime();
        byte[] data = Util.getRandomByteArray((int) Math.pow(2, 20));
        try {
//            Send operation type
            byte[] operationTypeBytes = Util.leIntToByteArray(3);
            int length = operationTypeBytes.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(operationTypeBytes);

//            Send number of blocks, just to follow the protocol
            byte[] numberOfBlocks = Util.leIntToByteArray(0);
            length = numberOfBlocks.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(numberOfBlocks);

//            Send data
            length = data.length;
            dataOutputStream.write(Util.beIntToByteArray(length));
            dataOutputStream.write(data);
            dataOutputStream.flush();

            byte[] res = new byte[length];
            length = dataInputStream.readInt();
            if (length > 0)
                dataInputStream.readFully(res, 0, length);

            if (!Arrays.equals(res, data)) {
                Util.logAndPrint(logger, "Received data unequal to original data");
                return -1;
            }
        } catch (IOException e) {
            logger.error("Error happened while reading block: " + e);
            logger.debug("Stacktrace", e);
            return -1;
        }
        return System.nanoTime() - startTime;
    }

    private boolean setupConnection(String ipAddress) {
        try {
            socket = new Socket(ipAddress, Constants.PORT);
            System.out.println("Socket opened, inet address: " + socket.getInetAddress());
            logger.info("Socket opened, inet address: " + socket.getInetAddress());
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
