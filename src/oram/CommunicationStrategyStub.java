package oram;

import oram.block.BlockEncrypted;
import oram.block.BlockLookahead;
import oram.blockcreator.LookaheadBlockCreator;
import oram.blockcreator.PathBlockCreator;
import oram.blockcreator.TrivialBlockCreator;
import oram.clientcom.CommunicationStrategy;
import oram.lookahead.AccessStrategyLookahead;
import oram.ofactory.ORAMFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 25-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class CommunicationStrategyStub implements CommunicationStrategy {
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

    public CommunicationStrategyStub(List<ORAMFactory> factories, int numberOfORAMLayers) {
        List<String> addresses;
        if (factories.size() == 1) {
            ORAMFactory oramFactory = factories.get(0);
            addresses = Util.getAddressStrings(0, oramFactory.getTotalSize());
            switch (oramFactory.getClass().getSimpleName()) {
                case "ORAMFactoryLookahead":
                case "ORAMFactoryLookaheadTrivial":
                    blocks = new LookaheadBlockCreator().createBlocks(addresses).toArray(new BlockEncrypted[0]);
                    return;
                case "ORAMFactoryPath":
                    blocks = new PathBlockCreator().createBlocks(addresses).toArray(new BlockEncrypted[0]);
                    return;
                default:
                    blocks = new TrivialBlockCreator().createBlocks(addresses).toArray(new BlockEncrypted[0]);
                    return;
            }
        }

        int offset = 0;
        int newOffset;

        List<BlockEncrypted> blocksList = new ArrayList<>();
        List<BlockEncrypted> blocksTmp;

        outer:
        for (int i = 0; i < factories.size(); i++) {
            int levelSize = (int) Math.pow(2, (((numberOfORAMLayers - 1) - i) * 4) + 6);
            switch (factories.get(i).getClass().getSimpleName()) {
                case "ORAMFactoryLookahead":
                    newOffset = offset + levelSize + (int) (2 * Math.sqrt(levelSize));
                    addresses = Util.getAddressStrings(offset, newOffset);
                    offset = newOffset;

                    blocksTmp = new LookaheadBlockCreator().createBlocks(addresses);
                    blocksList.addAll(blocksTmp);
                    break;
                case "ORAMFactoryLookaheadTrivial":
                    newOffset = (int) (offset + levelSize + (int) (2 * Math.sqrt(levelSize)) + (Math.ceil((double) levelSize / Constants.POSITION_BLOCK_SIZE)));
                    addresses = Util.getAddressStrings(offset, newOffset);
                    offset = newOffset;

                    blocksTmp = new LookaheadBlockCreator().createBlocks(addresses);
                    blocksList.addAll(blocksTmp);
                    break;
                case "ORAMFactoryPath":
                    newOffset = offset + (levelSize - 1) * Constants.DEFAULT_BUCKET_SIZE;
                    addresses = Util.getAddressStrings(offset, newOffset);
                    offset = newOffset;

                    blocksTmp = new PathBlockCreator().createBlocks(addresses);
                    blocksList.addAll(blocksTmp);
                    break;
                default:
                    newOffset = offset + levelSize + 1;
                    addresses = Util.getAddressStrings(offset, newOffset);

                    blocksTmp = new TrivialBlockCreator().createBlocks(addresses);
                    blocksList.addAll(blocksTmp);
                    break outer;
            }
        }
        blocks = blocksList.toArray(new BlockEncrypted[0]);
    }

    @Override
    public boolean start() {
        return true;
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

    @Override
    public boolean sendEndSignal() {
        return true;
    }

    @Override
    public long speedTest() {
        return 0;
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
