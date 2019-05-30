package oram.blockcreator;

import oram.Constants;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockPath;
import oram.encryption.EncryptionStrategyImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

/**
 * <p> oram_server <br>
 * Created by Christoffer S. Jensen on 09-04-2019. <br>
 * Master Thesis 2019 </p>
 */

public class PathBlockCreator implements BlockCreator {
    private static final Logger logger = LogManager.getLogger("log");

    @Override
    public List<BlockEncrypted> createBlocks(List<String> addresses) {
        if (addresses == null) {
            Util.logAndPrint(logger, "Addresses were null");
            return null;
        }
        if (addresses.isEmpty()) {
            Util.logAndPrint(logger, "Addresses were empty");
            return new ArrayList<>();
        }

        List<BlockPath> blocks = new ArrayList<>();
        int numberOfFiles = addresses.size();
        Util.logAndPrint(logger, "Overwriting " + numberOfFiles + " Path files, from: " + addresses.get(0) + ", to: " + addresses.get(addresses.size() - 1));
        for (String ignored : addresses)
            blocks.add(getPathDummyBlock());

        Util.logAndPrint(logger, "    " + numberOfFiles + " dummy blocks created");

        EncryptionStrategyImpl encryptionStrategy = new EncryptionStrategyImpl();
        List<BlockEncrypted> encryptedList = encryptBlocks(blocks, encryptionStrategy,
                encryptionStrategy.generateSecretKey(Constants.KEY_BYTES));

        if (encryptedList.isEmpty()) return null;
        return encryptedList;
    }

    private List<BlockEncrypted> encryptBlocks(List<BlockPath> blockPaths, EncryptionStrategyImpl encryptionStrategy,
                                               SecretKey secretKey) {
        List<BlockEncrypted> res = new ArrayList<>();
        for (int i = 0; i < blockPaths.size(); i++) {
            BlockPath block = blockPaths.get(i);
            if (block == null) {
                res.add(null);
                continue;
            }

            byte[] addressCipher = encryptionStrategy.encrypt(Util.leIntToByteArray(block.getAddress()), secretKey);
            byte[] indexCipher = encryptionStrategy.encrypt(Util.leIntToByteArray(block.getIndex()), secretKey);
            byte[] dataCipher = encryptionStrategy.encrypt(block.getData(), secretKey);
            if (addressCipher == null || indexCipher == null || dataCipher == null) {
                logger.error("Unable to encrypt address: " + block.getAddress() + " or data");
                return new ArrayList<>();
            }

            byte[] encryptedDataPlus = new byte[indexCipher.length + dataCipher.length];
            System.arraycopy(dataCipher, 0, encryptedDataPlus, 0, dataCipher.length);
            System.arraycopy(indexCipher, 0, encryptedDataPlus, dataCipher.length, indexCipher.length);

            res.add(new BlockEncrypted(addressCipher, encryptedDataPlus));

            double percent = ((double) (i + 1) / blockPaths.size()) * 100;
            if (percent % 1 == 0)
                Util.logAndPrint(logger, "    Done with encrypting " + ((int) percent) + "% of the files");
        }
        return res;
    }

    private BlockPath getPathDummyBlock() {
        return new BlockPath(0, new byte[Constants.BLOCK_SIZE], 0);
    }
}
