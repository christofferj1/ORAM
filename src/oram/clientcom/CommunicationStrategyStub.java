package oram.clientcom;

import oram.Constants;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockLookahead;
import oram.blockcreator.LookaheadBlockCreator;
import oram.blockcreator.PathBlockCreator;
import oram.blockcreator.TrivialBlockCreator;
import oram.blockenc.BlockEncryptionStrategyLookahead;
import oram.ofactory.ORAMFactory;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 25-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class CommunicationStrategyStub implements CommunicationStrategy {
    private BlockEncrypted[] blocks;
    private int bucketSize; // Matrix height (+2) when using Lookahead ORAM

    public CommunicationStrategyStub(int size, int bucketSize) {
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
                case "ORAMFactoryLookaheadMult":
                case "ORAMFactoryLookaheadTrivial":
                    blocks = new LookaheadBlockCreator().createBlocks(addresses).toArray(new BlockEncrypted[0]);
                    return;
                case "ORAMFactoryPath":
                case "ORAMFactoryPathMult":
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

    public static String printTreeEncrypted(BlockEncrypted[] array, int bucketSize) {
        int layers = 0;
        while ((array.length / bucketSize) >= Math.pow(2, layers)) {
            layers++;
        }

        return printBucketEncrypted(array, bucketSize, 0, 1, layers);
    }

    public static String printBucketEncrypted(BlockEncrypted[] array, int bucketSize, int index, int layer,
                                              int maxLayers) {
        StringBuilder prefix = new StringBuilder();
        for (int i = 1; i < layer; i++) {
            prefix.append("        ");
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bucketSize; i++) {
            int firstIndexInBucket = index * bucketSize;
            int currentIndex = firstIndexInBucket + i;
            if (i == 0)
                builder.append(prefix).append(StringUtils.leftPad(String.valueOf(index), 2)).append(": ");
            else
                builder.append(prefix).append("    ");
            if (array.length > currentIndex)
                builder.append(array[currentIndex].toStringShort());
            builder.append("\n");
        }

        if (index >= Math.pow(2, maxLayers - 1) - 1)
            return builder.toString();


        String rightChild;
        String leftChild;
        if (index == 0) {
            rightChild = printBucketEncrypted(array, bucketSize, 2, layer + 1, maxLayers);
            leftChild = printBucketEncrypted(array, bucketSize, 1, layer + 1, maxLayers);
        } else {
            rightChild = printBucketEncrypted(array, bucketSize, ((index + 1) * 2), layer + 1, maxLayers);
            leftChild = printBucketEncrypted(array, bucketSize, ((index + 1) * 2) - 1, layer + 1, maxLayers);
        }

        builder.insert(0, rightChild);
        builder.append(leftChild);

        return builder.toString();
    }

    @Override
    public boolean start(String ipAddress) {
        return true;
    }

    @Override
    public BlockEncrypted read(int address) {
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

//    The methods beneath are used to print the Path ORAM tree and Lookahead ORAM matrix

    @Override
    public long speedTest() {
        return 0;
    }

    public void setBlocks(BlockEncrypted[] blocks) {
        this.blocks = blocks;
    }

    public String getMatrixAndStashString(BlockEncryptionStrategyLookahead blockEncStrategy, SecretKey secretKey) {
        BlockLookahead[] blocks = new BlockLookahead[this.blocks.length];
        for (int i = 0; i < this.blocks.length; i++)

            blocks[i] = blockEncStrategy.decryptBlock(this.blocks[i], secretKey);

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
}
