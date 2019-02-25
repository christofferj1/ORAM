package oram.util;

import oram.*;
import oram.server.Server;

import java.util.ArrayList;
import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 25-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class ServerStub implements Server {
    List<BlockEncrypted> blocks;

    public ServerStub(int size, String key) {
        blocks = new ArrayList<>();
        for (int i = 0; i <= size; i++) {
            byte[] address = AES.encrypt(Util.sizedByteArrayWithInt(0, Constants.LOG_OF_BLOCK_SIZE), key);
            byte[] data = AES.encrypt(Util.sizedByteArrayWithInt(0, Constants.BLOCK_SIZE), key);
            blocks.add(new BlockEncrypted(address, data));
        }
//        blocks = new ArrayList<>(Arrays.asList(new BlockEncrypted[size]));
    }

    @Override
    public Block read(int address) {
        return (Block) blocks.get(address);
    }

    @Override
    public boolean write(int address, BlockEncrypted block) {
        return false;
    }
}
