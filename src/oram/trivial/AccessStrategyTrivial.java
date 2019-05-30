package oram.trivial;

import oram.AccessStrategy;
import oram.Constants;
import oram.OperationType;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockTrivial;
import oram.blockenc.BlockEncryptionStrategyTrivial;
import oram.clientcom.CommunicationStrategy;
import oram.factory.Factory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
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
    private final BlockEncryptionStrategyTrivial blockEncStrategy;
    private final List<Integer> allAddresses;
    private String prefixString;

    public AccessStrategyTrivial(int size, byte[] key, Factory factory, int offset, int prefixSize) {
        this.communicationStrategy = factory.getCommunicationStrategy();
        blockEncStrategy = factory.getBlockEncryptionStrategyTrivial();
        this.secretKey = factory.getEncryptionStrategy().generateSecretKey(key);
        this.allAddresses = IntStream.range(offset, offset + size + 1).boxed().collect(Collectors.toList());
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
        List<BlockTrivial> blocks = blockEncStrategy.decryptBlocks(encryptedBlocks, secretKey);

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
        List<BlockEncrypted> blocksToWrite = blockEncStrategy.encryptBlocks(blocks, secretKey);

        boolean writeStatus = communicationStrategy.writeArray(allAddresses, blocksToWrite);
        if (!writeStatus) return null;

        return res;
    }
}
