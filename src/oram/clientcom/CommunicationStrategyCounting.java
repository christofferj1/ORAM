package oram.clientcom;

import oram.block.BlockEncrypted;

import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 04-04-2019. <br>
 * Master Thesis 2019 </p>
 */

public class CommunicationStrategyCounting implements CommunicationStrategy {
    private final CommunicationStrategy communicationStrategy;
    private int blocksSent;

    public CommunicationStrategyCounting(CommunicationStrategy communicationStrategy) {
        this.communicationStrategy = communicationStrategy;
        blocksSent = 0;
    }

    @Override
    public boolean start() {
        return communicationStrategy.start();
    }

    @Override
    public BlockEncrypted read(int address) {
        return communicationStrategy.read(address);
    }

    @Override
    public boolean write(int address, BlockEncrypted block) {
        boolean write = communicationStrategy.write(address, block);
        blocksSent++;
        return write;
    }

    @Override
    public List<BlockEncrypted> readArray(List<Integer> addresses) {
        return communicationStrategy.readArray(addresses);
    }

    @Override
    public boolean writeArray(List<Integer> addresses, List<BlockEncrypted> blocks) {
        boolean b = communicationStrategy.writeArray(addresses, blocks);
        blocksSent += addresses.size();
        return b;
    }

    @Override
    public boolean sendEndSignal() {
        return communicationStrategy.sendEndSignal();
    }

    @Override
    public long speedTest() {
        return communicationStrategy.speedTest();
    }

    public int getBlocksSent() {
        return blocksSent;
    }
}
