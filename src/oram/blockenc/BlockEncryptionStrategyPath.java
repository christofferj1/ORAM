package oram.blockenc;

import oram.Constants;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockPath;
import oram.encryption.EncryptionStrategy;
import oram.permutation.PermutationStrategy;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 30-05-2019. <br>
 * Master Thesis 2019 </p>
 */

public class BlockEncryptionStrategyPath {
    private static final Logger logger = LogManager.getLogger("log");
    private EncryptionStrategy encryptionStrategy;
    private PermutationStrategy permutationStrategy;

    public BlockEncryptionStrategyPath(EncryptionStrategy encryptionStrategy, PermutationStrategy permutationStrategy) {
        this.encryptionStrategy = encryptionStrategy;
        this.permutationStrategy = permutationStrategy;
    }

    public List<BlockEncrypted> encryptBlocks(List<BlockPath> blocks, SecretKey secretKey) {
        List<BlockEncrypted> encryptedBlocksToWrite = new ArrayList<>();

        for (BlockPath block : blocks) {
            byte[] addressCipher = encryptionStrategy.encrypt(Util.leIntToByteArray(block.getAddress()), secretKey);
            byte[] indexCipher = encryptionStrategy.encrypt(Util.leIntToByteArray(block.getIndex()), secretKey);
            byte[] dataCipher = encryptionStrategy.encrypt(block.getData(), secretKey);
            if (addressCipher == null || indexCipher == null || dataCipher == null) {
                logger.error("Unable to encrypt address: " + block.getAddress() + " or data");
                return null;
            }
            byte[] encryptedDataPlus = ArrayUtils.addAll(dataCipher, indexCipher);
            encryptedBlocksToWrite.add(new BlockEncrypted(addressCipher, encryptedDataPlus));
        }
        encryptedBlocksToWrite = permutationStrategy.permuteEncryptedBlocks(encryptedBlocksToWrite);
        return encryptedBlocksToWrite;
    }

    public List<BlockPath> decryptBlocks(List<BlockEncrypted> blocks, SecretKey secretKey, boolean filterOutDummies) {
        List<BlockPath> res = new ArrayList<>();
        for (BlockEncrypted block : blocks) {
            if (block == null) continue;
//            Address
            byte[] addressBytes = encryptionStrategy.decrypt(block.getAddress(), secretKey);

            if (addressBytes == null) continue;
            int addressInt = Util.byteArrayToLeInt(addressBytes);

            if (filterOutDummies && Util.isDummyAddress(addressInt)) continue;

//            Data and index
            byte[] encryptedDataFull = block.getData();

            int encryptedDataFullLength = encryptedDataFull.length;
            int endOfDataIndex = encryptedDataFullLength - Constants.AES_BLOCK_SIZE * 2;

            byte[] encryptedData = Arrays.copyOfRange(encryptedDataFull, 0, endOfDataIndex);
            byte[] encryptedIndex = Arrays.copyOfRange(encryptedDataFull, endOfDataIndex, encryptedDataFullLength);

            byte[] data = encryptionStrategy.decrypt(encryptedData, secretKey);
            byte[] indexBytes = encryptionStrategy.decrypt(encryptedIndex, secretKey);

            if (data == null || indexBytes == null) {
                Util.logAndPrint(logger, "Unable to decrypt a Path block");
                return null;
            }

            int index = Util.byteArrayToLeInt(indexBytes);

            res.add(new BlockPath(addressInt, data, index));
        }
        return res;
    }
}
