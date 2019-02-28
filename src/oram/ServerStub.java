package oram;

import oram.server.Server;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 25-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class ServerStub implements Server {
    //    List<BlockEncrypted> blocks;
    private BlockEncrypted[] blocks;
    private int bucketSize;

    public ServerStub(int size, int bucketSize) {
//        blocks = new ArrayList<>();
//        for (int i = 0; i < (size * bucketSize); i++) {
//            byte[] address = AES.encrypt(Util.sizedByteArrayWithInt(0, Constants.LOG_OF_BLOCK_SIZE), key);
//            byte[] data = AES.encrypt(Util.sizedByteArrayWithInt(0, Constants.BLOCK_SIZE), key);
//            blocks.add(new BlockEncrypted(address, data));
//        }
//        blocks = new ArrayList<>(Arrays.asList(new BlockEncrypted[size]));
        this.bucketSize = bucketSize;
        blocks = new BlockEncrypted[size * bucketSize];
    }

    @Override
    public BlockEncrypted read(int address) {
//        return blocks.get(address);
        return blocks[address];
    }

    @Override
    public boolean write(int address, BlockEncrypted block) {
        if (address >= blocks.length)
            return true;

        blocks[address] = block;
        return true;
    }

    public String getTreeString() {
        return Util.printTree(blocks, bucketSize);
    }
}