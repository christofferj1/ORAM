package oram.server;

import oram.path.BlockPath;

import java.util.Arrays;
import java.util.Objects;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 11-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class BlockServer {
    private Integer address;
    private byte[] data;

    public BlockServer() {
    }

    public BlockServer(int address, byte[] data) {
        this.address = address;
        this.data = data;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockPath blockPath = (BlockPath) o;
        return getAddress() == blockPath.getAddress() &&
                Arrays.equals(getData(), blockPath.getData());
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(getAddress());
        result = 31 * result + Arrays.hashCode(getData());
        return result;
    }

    @Override
    public String toString() {
        return "BlockPath{" +
                "address=" + address +
                ", data=" + Arrays.toString(data) +
                '}';
    }

}
