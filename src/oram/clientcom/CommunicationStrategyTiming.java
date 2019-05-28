package oram.clientcom;

import oram.block.BlockEncrypted;

import java.util.List;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 04-04-2019. <br>
 * Master Thesis 2019 </p>
 */

public class CommunicationStrategyTiming implements CommunicationStrategy {
    private final CommunicationStrategy communicationStrategy;
    private long time;

    public CommunicationStrategyTiming(CommunicationStrategy communicationStrategy) {
        this.communicationStrategy = communicationStrategy;
        time = 0;
    }

    @Override
    public boolean start(String ipAddress) {
        return communicationStrategy.start(ipAddress);
    }

    @Override
    public BlockEncrypted read(int address) {
        long startTime = System.nanoTime();
        BlockEncrypted read = communicationStrategy.read(address);
        time += System.nanoTime() - startTime;
        return read;
    }

    @Override
    public boolean write(int address, BlockEncrypted block) {
        long startTime = System.nanoTime();
        boolean write = communicationStrategy.write(address, block);
        time += System.nanoTime() - startTime;
        return write;
    }

    @Override
    public List<BlockEncrypted> readArray(List<Integer> addresses) {
        long startTime = System.nanoTime();
        List<BlockEncrypted> encryptedList = communicationStrategy.readArray(addresses);
        time += System.nanoTime() - startTime;
        return encryptedList;
    }

    @Override
    public boolean writeArray(List<Integer> addresses, List<BlockEncrypted> blocks) {
        long startTime = System.nanoTime();
        boolean b = communicationStrategy.writeArray(addresses, blocks);
        time += System.nanoTime() - startTime;
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

    public long getTime() {
        return time;
    }
}
