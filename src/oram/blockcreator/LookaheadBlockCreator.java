package oram.blockcreator;

import oram.Constants;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockLookahead;
import oram.encryption.EncryptionStrategyImpl;
import oram.lookahead.Index;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

/**
 * <p> oram_server <br>
 * Created by Christoffer S. Jensen on 04-04-2019. <br>
 * Master Thesis 2019 </p>
 */

public class LookaheadBlockCreator implements BlockCreator {
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

        List<BlockLookahead> blocks = new ArrayList<>();
        int numberOfFiles = addresses.size();
        Util.logAndPrint(logger, "Overwriting " + numberOfFiles + " Lookahead files, from: " + addresses.get(0) + ", to: " + addresses.get(addresses.size() - 1));
        for (String ignored : addresses)
            blocks.add(getLookaheadDummyBlock());

        Util.logAndPrint(logger, "    " + numberOfFiles + " dummy blocks created");

        EncryptionStrategyImpl encryptionStrategy = new EncryptionStrategyImpl();
        List<BlockEncrypted> encryptedList = encryptBlocks(blocks, encryptionStrategy,
                encryptionStrategy.generateSecretKey(Constants.KEY_BYTES));

        if (encryptedList.isEmpty()) return null;

        Util.logAndPrint(logger, "    Data encrypted");

        return encryptedList;

//        List<BlockEncrypted> res = new ArrayList<>();
//        for (int i = 0; i < numberOfFiles; i++) {
//            byte[] data = encryptedList.get(i).getData();
//            byte[] address = encryptedList.get(i).getAddress();
//            byte[] bytesToWrite = new byte[data.length + address.length];
//            System.arraycopy(address, 0, bytesToWrite, 0, address.length);
//            System.arraycopy(data, 0, bytesToWrite, address.length, data.length);
//
//            if (!Util.writeFile(bytesToWrite, addresses.get(i))) {
//                logger.error("Unable to write file: " + i);
//                return false;
//            }
//
//
//
//            double percent = ((double) (i + 1) / numberOfFiles) * 100;
//            if (percent % 1 == 0)
//                Util.logAndPrint(logger,"    Done with " + ((int) percent) + "% of the files");
//        }
//
//        return true;
    }

    private List<BlockEncrypted> encryptBlocks(List<BlockLookahead> blockLookaheads,
                                               EncryptionStrategyImpl encryptionStrategy, SecretKey secretKey) {
        List<BlockEncrypted> res = new ArrayList<>();
        for (int i = 0; i < blockLookaheads.size(); i++) {
            BlockLookahead block = blockLookaheads.get(i);
            if (block == null) {
                res.add(null);
                continue;
            }
            byte[] rowIndexBytes = Util.leIntToByteArray(block.getRowIndex());
            byte[] colIndexBytes = Util.leIntToByteArray(block.getColIndex());
            byte[] addressBytes = Util.leIntToByteArray(block.getAddress());
            byte[] encryptedAddress = encryptionStrategy.encrypt(addressBytes, secretKey);
            byte[] encryptedData = encryptionStrategy.encrypt(block.getData(), secretKey);

            byte[] indexBytes = new byte[rowIndexBytes.length + colIndexBytes.length];
            System.arraycopy(rowIndexBytes, 0, indexBytes, 0, rowIndexBytes.length);
            System.arraycopy(colIndexBytes, 0, indexBytes, rowIndexBytes.length, colIndexBytes.length);
            byte[] encryptedIndex = encryptionStrategy.encrypt(indexBytes, secretKey);

            if (encryptedAddress == null || encryptedData == null || encryptedIndex == null) {
                logger.error("Unable to encrypt block: " + block.toStringShort());
                return new ArrayList<>();
            }

            byte[] encryptedDataPlus = new byte[encryptedData.length + encryptedIndex.length];
            System.arraycopy(encryptedData, 0, encryptedDataPlus, 0, encryptedData.length);
            System.arraycopy(encryptedIndex, 0, encryptedDataPlus, encryptedData.length, encryptedIndex.length);

            res.add(new BlockEncrypted(encryptedAddress, encryptedDataPlus));

            double percent = ((double) (i + 1) / blockLookaheads.size()) * 100;
            if (percent % 1 == 0)
                Util.logAndPrint(logger, "    Done with " + ((int) percent) + "% of the files");
        }
        return res;
    }

    private BlockLookahead getLookaheadDummyBlock() {
        BlockLookahead blockLookahead = new BlockLookahead(0, new byte[Constants.BLOCK_SIZE]);
        blockLookahead.setIndex(new Index(0, 0));
        return blockLookahead;
    }
}
