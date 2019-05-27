package oram.trivial;

import oram.AccessStrategy;
import oram.Constants;
import oram.OperationType;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockTrivial;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.factory.Factory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 27-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class AccessStrategyTrivial implements AccessStrategy {
    private final Logger logger = LogManager.getLogger("log");
    private final SecretKey secretKey;
    private final CommunicationStrategy communicationStrategy;
    private final EncryptionStrategy encryptionStrategy;
    private final List<Integer> allAddresses;
    private String prefixString;

    public AccessStrategyTrivial(int size, byte[] key, Factory factory, int offset, int prefixSize) {
        this.communicationStrategy = factory.getCommunicationStrategy();
        this.encryptionStrategy = factory.getEncryptionStrategy();
        this.secretKey = encryptionStrategy.generateSecretKey(key);
        this.allAddresses = IntStream.range(offset, offset + size).boxed().collect(Collectors.toList());
        prefixString = Util.getEmptyStringOfLength(prefixSize);
    }

    @Override
    public boolean setup(List<BlockTrivial> blocks) {
        return true;
    }

    @Override
    public byte[] access(OperationType op, int address, byte[] data, boolean recursiveLookup, boolean lookaheadSetup) {
        logger.info(prefixString + "Access op: " + op.toString() + ", address: " + address + ", read addresses from " +
                allAddresses.get(0) + " to " + allAddresses.get(allAddresses.size() - 1));

        List<BlockEncrypted> encryptedBlocks = communicationStrategy.readArray(allAddresses);
        List<BlockTrivial> blocks = decryptBlocks(encryptedBlocks);

        int addressToLookUp = address;
        if (recursiveLookup)
            addressToLookUp = (int) Math.ceil((double) address / Constants.POSITION_BLOCK_SIZE);

        BlockTrivial block = blocks.get(addressToLookUp);
        byte[] res = block.getData();
        if (op.equals(OperationType.WRITE)) {
            if (recursiveLookup) {
                Map<Integer, Integer> map;
                if (block.getAddress() == 0)
                    map = Util.getDummyMap(address);
                else
                    map = Util.getMapFromByteArray(res);

                res = Util.getByteArrayFromMap(map);

                if (map == null)
                    return null;
                map.put(address, Util.byteArrayToLeInt(data));
                blocks.get(addressToLookUp).setData(Util.getByteArrayFromMap(map));
                blocks.get(addressToLookUp).setAddress(addressToLookUp);
            } else {
                blocks.get(addressToLookUp).setData(data);
                blocks.get(addressToLookUp).setAddress(addressToLookUp);
            }
        }
        List<BlockEncrypted> blocksToWrite = encryptBlocks(blocks);

        boolean writeStatus = communicationStrategy.writeArray(allAddresses, blocksToWrite);
        if (!writeStatus) return null;

        return res;
    }

    private List<BlockTrivial> decryptBlocks(List<BlockEncrypted> blocks) {
        List<BlockTrivial> res = new ArrayList<>();
        for (BlockEncrypted b : blocks) {
            byte[] addressBytes = encryptionStrategy.decrypt(b.getAddress(), secretKey);
            byte[] data = encryptionStrategy.decrypt(b.getData(), secretKey);

            if (addressBytes == null || data == null) {
                logger.error(prefixString + "Unable to decrypt either address or data");
                res = null;
                break;
            }

            int address = Util.byteArrayToLeInt(addressBytes);
            res.add(new BlockTrivial(address, data));
        }
        return res;
    }

    private List<BlockEncrypted> encryptBlocks(List<BlockTrivial> blocks) {
        List<BlockEncrypted> res = new ArrayList<>();
        for (BlockTrivial b : blocks) {
            byte[] addressBytes = Util.leIntToByteArray(b.getAddress());
            byte[] addressCipher = encryptionStrategy.encrypt(addressBytes, secretKey);
            byte[] dataCipher = encryptionStrategy.encrypt(b.getData(), secretKey);

            if (addressCipher == null || dataCipher == null) {
                logger.error(prefixString + "Unable to encrypt either address or data");
                res = null;
                break;
            }

            res.add(new BlockEncrypted(addressCipher, dataCipher));
        }
        return res;
    }
}
