package oram;

import oram.block.BlockEncrypted;
import oram.block.BlockLookahead;
import oram.clientcom.CommunicationStrategy;
import oram.lookahead.AccessStrategyLookahead;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<BlockEncrypted> readArray(List<Integer> addresses) {
        List<BlockEncrypted> res = new ArrayList<>();
        for (Integer i : addresses) {
            res.add(blocks[i]);
        }
        return res;
    }

    @Override
    public boolean writeArray(List<Integer> addresses, List<BlockEncrypted> blocks) {
        for (int i = 0; i < addresses.size(); i++) {
            this.blocks[addresses.get(i)] = blocks.get(i);
        }
        return true;
    }

    public String getTreeString() {
        return Util.printTreeEncrypted(blocks, bucketSize);
    }

    public String getMatrixAndStashString(AccessStrategyLookahead accessStrategy) {
        BlockLookahead[] blocks = new BlockLookahead[this.blocks.length];
        for (int i = 0; i < this.blocks.length; i++)
            blocks[i] = accessStrategy.decryptToLookaheadBlock(this.blocks[i]);

        StringBuilder builder = new StringBuilder("\n#### Printing matrix and swaps ####\n");
        for (int row = 0; row < bucketSize; row++) {
            for (int col = 0; col < bucketSize; col++) {
                int index = col * 4 + row;
                BlockLookahead block = blocks[index];
                if (block != null) {
                    String string = new String(block.getData()).trim();
                    builder.append(StringUtils.rightPad(string.isEmpty() ? "null" : string, 12));
                    builder.append(";");
                    builder.append(StringUtils.leftPad(Integer.toString(block.getAddress()).trim(), 3));
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
                String string = new String(block.getData()).trim();
                builder.append(StringUtils.rightPad(string.isEmpty() ? "null" : string, 12));
                builder.append(", at ").append(StringUtils.leftPad(String.valueOf(block.getAddress()), 2)).append(" ");
                builder.append("(");
                builder.append(StringUtils.leftPad(Integer.toString(block.getRowIndex()).trim(), 2));
                builder.append(", ");
                builder.append(StringUtils.leftPad(Integer.toString(block.getColIndex()).trim(), 2));
                builder.append(")");
            } else
                builder.append("                     null");
            builder.append(" : ");
            index -= bucketSize;
            block = blocks[index];
            if (block != null) {
                String string = new String(block.getData()).trim();
                builder.append(StringUtils.rightPad(string.isEmpty() ? "null" : string, 12));
                builder.append(", at ").append(StringUtils.leftPad(String.valueOf(block.getAddress()), 2)).append(" ");
                builder.append("(");
                builder.append(StringUtils.leftPad(Integer.toString(block.getRowIndex()).trim(), 1));
                builder.append(", ");
                builder.append(StringUtils.leftPad(Integer.toString(block.getColIndex()).trim(), 1));
                builder.append(") ");
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
