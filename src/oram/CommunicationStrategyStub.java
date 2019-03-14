package oram;

import oram.clientcom.CommunicationStrategy;
import oram.lookahead.AccessStrategyLookahead;
import oram.lookahead.BlockLookahead;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 25-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class CommunicationStrategyStub implements CommunicationStrategy {
    //    List<BlockEncrypted> blocks;
    private BlockEncrypted[] blocks;
    private int bucketSize;

    public CommunicationStrategyStub(int size, int bucketSize) {
//        blocks = new ArrayList<>();
//        for (int i = 0; i < (size * bucketSize); i++) {
//            byte[] address = EncryptionStrategy.encrypt(Util.sizedByteArrayWithInt(0, Constants.LOG_OF_BLOCK_SIZE), key);
//            byte[] data = EncryptionStrategy.encrypt(Util.sizedByteArrayWithInt(0, Constants.BLOCK_SIZE), key);
//            blocks.add(new BlockEncrypted(address, data));
//        }
//        blocks = new ArrayList<>(Arrays.asList(new BlockEncrypted[size]));
        this.bucketSize = bucketSize;
        blocks = new BlockEncrypted[size * bucketSize];
    }

    @Override
    public boolean start() {
        return false;
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

    public String getMatrixAndStashString(AccessStrategyLookahead accessStrategy) {
        System.out.println(Arrays.toString(accessStrategy.secretKey.getEncoded()));
//        AccessStrategyLookahead access = new AccessStrategyLookahead(0, 0, "bytes".getBytes(), new FactoryTest(0, 0));
        BlockLookahead[] blocks = new BlockLookahead[this.blocks.length];
        for (int i = 0; i < this.blocks.length; i++) {
//            if (this.blocks[i] == null || this.blocks[i].getAddress() == 0)
//                System.out.println("WHAAAAAT??!?");
            BlockLookahead blockLookahead = accessStrategy.decryptToLookaheadBlock(this.blocks[i]);
            if (blockLookahead == null)
                System.out.println("Sheeeeeiit");
            blocks[i] = blockLookahead;
        }

        StringBuilder builder = new StringBuilder("\n#### Printing matrix and swaps ####\n");
        for (int row = 0; row < bucketSize; row++) {
            for (int col = 0; col < bucketSize; col++) {
                int index = col * 4 + row;
                BlockLookahead block = blocks[index];
                if (block != null) {
//                    if (Util.isDummyAddress(block.getAddress()))
//                        builder.append("dummy            ");
//                    else {
                        String string = new String(block.getData()).trim();
                        builder.append(StringUtils.rightPad(string.isEmpty() ? "null" : string, 12));
                        builder.append(";");
                        builder.append(StringUtils.leftPad(Integer.toString(block.getAddress()).trim(), 3));
//                    }
                } else
                    builder.append("       null");
                builder.append(" : ");
            }
            builder.append("\n");
        }

        builder.append("Swap                         : Access\n");
        for (int i = 0; i < bucketSize; i++) {
            int index = i + bucketSize * bucketSize + bucketSize;
            BlockLookahead block = blocks[index];
            if (block != null) {
//                if (Util.isDummyAddress(block.getAddress()))
//                    builder.append("dummy                ");
//                else {
                    String string = new String(block.getData()).trim();
                    builder.append(StringUtils.rightPad(string.isEmpty() ? "null" : string, 12));
                builder.append(", at ").append(StringUtils.leftPad(String.valueOf(block.getAddress()),2)).append(" ");
                    builder.append("(");
                    builder.append(StringUtils.leftPad(Integer.toString(block.getRowIndex()).trim(), 2));
                    builder.append(", ");
                    builder.append(StringUtils.leftPad(Integer.toString(block.getColIndex()).trim(), 2));
                    builder.append(")");
//                }
            } else
                builder.append("                     null");
            builder.append(" : ");
            index -= bucketSize;
            block = blocks[index];
            if (block != null) {
//                if (Util.isDummyAddress(block.getAddress()))
//                    builder.append("dummy");
//                else {
                    String string = new String(block.getData()).trim();
                    builder.append(StringUtils.rightPad(string.isEmpty() ? "null" : string, 12));
                    builder.append(", at ").append(StringUtils.leftPad(String.valueOf(block.getAddress()),2)).append(" ");
                    builder.append("(");
                    builder.append(StringUtils.leftPad(Integer.toString(block.getRowIndex()).trim(), 1));
                    builder.append(", ");
                    builder.append(StringUtils.leftPad(Integer.toString(block.getColIndex()).trim(), 1));
                    builder.append(") ");
//                }
            } else
                builder.append("               null");

            builder.append("\n");
        }

        return builder.toString();
    }

    public void setBlocks(BlockEncrypted[] blocks) {
        this.blocks = blocks;
    }
}
