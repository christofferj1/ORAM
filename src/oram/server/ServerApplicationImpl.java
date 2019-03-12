package oram.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 11-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class ServerApplicationImpl implements ServerApplication {
    private final Logger logger = LogManager.getLogger("log");

    @Override
    public List<BlockServer> read(List<Integer> addresses) {
        List<BlockServer> res = new ArrayList<>();
        for (Integer i : addresses) {
            byte[] data = readFile(Integer.toString(i));
            if (data == null) return null;

            res.add(new BlockServer(i, data));
        }
        return res;
    }

    @Override
    public boolean write(List<String> addresses, List<byte[]> dataArrays) {
        if (addresses.size() != dataArrays.size()) {
            logger.error("Not same number of addresses (" + addresses.size() + ") as data arrays (" + dataArrays.size()
                    + ")");
            return false;
        }
        for (int i = 0; i < addresses.size(); i++) {
            boolean success = writeFile(dataArrays.get(i), addresses.get(i));
            if (!success) return false;
        }
        return true;
    }

    private byte[] readFile(String fileName) {
        byte[] res;
        try {
            res = Files.readAllBytes(Paths.get(fileName));
        } catch (IOException e) {
            logger.error("Error happened while reading file: " + fileName + ", " + e);
            logger.debug("Stacktrace", e);
            return null;
        }
        return res;
    }

    private boolean writeFile(byte[] bytesForFile, String fileName) {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(bytesForFile);
        } catch (IOException e) {
            logger.error("Error happened while writing file: " + fileName + ", " + e);
            logger.debug("Stacktrace", e);
            return false;
        }
        return true;
    }
}