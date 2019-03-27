package oram.trivial;

import oram.AccessStrategy;
import oram.OperationType;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockStandard;
import oram.clientcom.CommunicationStrategy;
import oram.encryption.EncryptionStrategy;
import oram.factory.Factory;
import oram.permutation.PermutationStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
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
    private final int size;
    private final CommunicationStrategy communicationStrategy;
    private final EncryptionStrategy encryptionStrategy;
    private final PermutationStrategy permutationStrategy;
    private Map<Integer, Integer> positionMap;

    public AccessStrategyTrivial(SecretKey secretKey, int size, Factory factory) {
        this.secretKey = secretKey;
        this.size = size;
        this.communicationStrategy = factory.getCommunicationStrategy();
        this.encryptionStrategy = factory.getEncryptionStrategy();
        this.permutationStrategy = factory.getPermutationStrategy();
    }

    @Override
    public byte[] access(OperationType op, int address, byte[] data) {
        SecureRandom randomness = new SecureRandom();
        int position = positionMap.getOrDefault(address, randomness.nextInt(size));

        logger.info("Access op: " + op.toString() + ", address: " + address + ", position: " + position);
        System.out.println("Access op: " + op.toString() + ", address: " + address + ", position: " + position);

        List<Integer> allAddresses = IntStream.range(0, size).boxed().collect(Collectors.toList());
        System.out.println("Read addresses from " + allAddresses.get(0) + " to " + allAddresses.get(allAddresses.size() - 1));
        List<BlockEncrypted> encryptedBlocks = communicationStrategy.readArray(allAddresses);
        List<BlockStandard> blocks = decryptBlocks(encryptedBlocks);

        byte[] res = blocks.get(position).getData();
        if (op.equals(OperationType.WRITE))
            blocks.get(position).setData(data);

        List<BlockEncrypted> blocksToWrite = encryptBlocks(blocks);

        boolean writeStatus = communicationStrategy.writeArray(allAddresses, blocksToWrite);
        if (!writeStatus) return null;

        return res;
    }

    List<BlockStandard> decryptBlocks(List<BlockEncrypted> blocks) {
        List<BlockStandard> res = new ArrayList<>();
        for (BlockEncrypted b : blocks) {
            byte[] addressBytes = encryptionStrategy.decrypt(b.getAddress(), secretKey);
            byte[] data = encryptionStrategy.decrypt(b.getData(), secretKey);

            if (addressBytes == null || data == null) {
                logger.error("Unable to decrypt either address or data");
                res = null;
                break;
            }

            int address = Util.byteArrayToLeInt(addressBytes);
            res.add(new BlockStandard(address, data));
        }
        return res;
    }

    List<BlockEncrypted> encryptBlocks(List<BlockStandard> blocks) {
        List<BlockEncrypted> res = new ArrayList<>();
        for (BlockStandard b : blocks) {
            byte[] addressBytes = Util.leIntToByteArray(b.getAddress());
            byte[] addressCipher = encryptionStrategy.encrypt(addressBytes, secretKey);
            byte[] dataCipher = encryptionStrategy.encrypt(b.getData(), secretKey);

            if (addressCipher == null || dataCipher == null) {
                logger.error("Unable to encrypt either address or data");
                res = null;
                break;
            }

            res.add(new BlockEncrypted(addressCipher, dataCipher));
        }
        return res;
    }
}
