package oram.blockenc;

import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockTrivial;
import oram.encryption.EncryptionStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 30-05-2019. <br>
 * Master Thesis 2019 </p>
 */

public class BlockEncryptionStrategyTrivial {
    private static final Logger logger = LogManager.getLogger("log");
    private EncryptionStrategy encryptionStrategy;

    public BlockEncryptionStrategyTrivial(EncryptionStrategy encryptionStrategy) {
        this.encryptionStrategy = encryptionStrategy;
    }

    public List<BlockEncrypted> encryptBlocks(List<BlockTrivial> blocks, SecretKey secretKey) {
        List<BlockEncrypted> res = new ArrayList<>();
        for (BlockTrivial b : blocks) {
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

    public List<BlockTrivial> decryptBlocks(List<BlockEncrypted> blocks, SecretKey secretKey) {
        List<BlockTrivial> res = new ArrayList<>();
        for (BlockEncrypted b : blocks) {
            byte[] addressBytes = encryptionStrategy.decrypt(b.getAddress(), secretKey);
            byte[] data = encryptionStrategy.decrypt(b.getData(), secretKey);

            if (addressBytes == null || data == null) {
                logger.error("Unable to decrypt either address or data");
                res = null;
                break;
            }

            int address = Util.byteArrayToLeInt(addressBytes);
            res.add(new BlockTrivial(address, data));
        }
        return res;
    }
}
