package oram.path;

import oram.AccessStrategy;
import oram.Constants;
import oram.OperationType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 19-02-2019. <br>
 * Copyright (C) Lind Invest 2019 </p>
 */

public class AccessStrategyPath implements AccessStrategy {
    private static final Logger logger = LogManager.getLogger("log");

    public static void main(String[] args) {
        System.out.println("test log");
        logger.debug("DEBUG");
        logger.error("ERROR");
        logger.info("INFO");
        logger.warn("WARN");
    }

    @Override
    public byte[] access(OperationType op, byte[] address, Byte[] data) {
        if (data != null && data.length > Constants.BLOCK_SIZE);

        return null;
    }
}
