package oram.blockenc;

import oram.Constants;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockLookahead;
import oram.encryption.EncryptionStrategy;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static oram.Constants.INTEGER_BYTE_ARRAY_SIZE;
import static oram.Util.byteArrayToLeInt;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 30-05-2019. <br>
 * Master Thesis 2019 </p>
 */

public class BlockEncryptionStrategyLookahead {
    private static final Logger logger = LogManager.getLogger("log");
    private EncryptionStrategy encryptionStrategy;

    public BlockEncryptionStrategyLookahead(EncryptionStrategy encryptionStrategy) {
        this.encryptionStrategy = encryptionStrategy;
    }

    public List<BlockEncrypted> encryptBlocks(List<BlockLookahead> blocks, SecretKey secretKey) {
        List<BlockEncrypted> res = new ArrayList<>();
        for (BlockLookahead block : blocks) {
            if (block == null) {
                res.add(null);
                continue;
            }
            byte[] rowIndexBytes = Util.leIntToByteArray(block.getRowIndex());
            byte[] colIndexBytes = Util.leIntToByteArray(block.getColIndex());
            byte[] addressBytes = Util.leIntToByteArray(block.getAddress());
            byte[] encryptedAddress = encryptionStrategy.encrypt(addressBytes, secretKey);
            byte[] encryptedData = encryptionStrategy.encrypt(block.getData(), secretKey);
            byte[] indexBytes = ArrayUtils.addAll(rowIndexBytes, colIndexBytes);
            byte[] encryptedIndex = encryptionStrategy.encrypt(indexBytes,
                    secretKey);

            if (encryptedAddress == null || encryptedData == null || encryptedIndex == null) {
                logger.error("Unable to encrypt block: " + block.toStringShort());
                return new ArrayList<>();
            }

            byte[] encryptedDataPlus = ArrayUtils.addAll(encryptedData, encryptedIndex);

            res.add(new BlockEncrypted(encryptedAddress, encryptedDataPlus));
        }
        return res;
    }

    public BlockEncrypted encryptBlock(BlockLookahead block, SecretKey secretKey) {
        List<BlockEncrypted> encrypted = encryptBlocks(Collections.singletonList(block), secretKey);
        if (encrypted.isEmpty())
            return null;
        else
            return encrypted.get(0);
    }

    public List<BlockLookahead> decryptBlocks(List<BlockEncrypted> blocks, SecretKey secretKey) {
        List<BlockLookahead> res = new ArrayList<>();
        for (BlockEncrypted b : blocks) {
            byte[] encryptedDataFull = b.getData();
            int encryptedDataFullLength = encryptedDataFull.length;
            int endOfDataIndex = encryptedDataFullLength - Constants.AES_BLOCK_SIZE * 2;
            byte[] encryptedData = Arrays.copyOfRange(encryptedDataFull, 0, endOfDataIndex);
            byte[] encryptedIndex = Arrays.copyOfRange(encryptedDataFull, endOfDataIndex, encryptedDataFullLength);
            byte[] data = encryptionStrategy.decrypt(encryptedData, secretKey);
            byte[] indices = encryptionStrategy.decrypt(encryptedIndex, secretKey);

            if (data == null) {
                logger.info("Tried to turn an encrypted block with value = null into a Lookahead block");
                return null;
            }

            byte[] addressBytes = encryptionStrategy.decrypt(b.getAddress(), secretKey);

            int address = byteArrayToLeInt(addressBytes);

            int rowDataIndex = 0;
            int colDataIndex = INTEGER_BYTE_ARRAY_SIZE;
            byte[] rowIndexBytes = Arrays.copyOfRange(indices, rowDataIndex, colDataIndex);
            byte[] colIndexBytes = Arrays.copyOfRange(indices, colDataIndex, INTEGER_BYTE_ARRAY_SIZE * 2);

            BlockLookahead blockLookahead = new BlockLookahead();
            blockLookahead.setAddress(address);
            blockLookahead.setData(data);
            blockLookahead.setRowIndex(byteArrayToLeInt(rowIndexBytes));
            blockLookahead.setColIndex(byteArrayToLeInt(colIndexBytes));

            res.add(blockLookahead);
        }
        return res;
    }

    public BlockLookahead decryptBlock(BlockEncrypted block, SecretKey secretKey) {
        List<BlockLookahead> encrypted = decryptBlocks(Collections.singletonList(block), secretKey);
        if (encrypted.isEmpty())
            return null;
        else
            return encrypted.get(0);
    }
}
