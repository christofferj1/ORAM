package oram.block;

import oram.Util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 20-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class BlockTrivial implements Block, Serializable {
    private Integer address;
    private byte[] data;

    public BlockTrivial() {
    }

    public BlockTrivial(int address, byte[] data) {
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
        BlockTrivial blockTrivial = (BlockTrivial) o;
        return getAddress() == blockTrivial.getAddress() &&
                Arrays.equals(getData(), blockTrivial.getData());
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(getAddress());
        result = 31 * result + Arrays.hashCode(getData());
        return result;
    }

    @Override
    public String toString() {
        return "BlockTrivial{" +
                "address=" + address +
                ", data=" + Arrays.toString(data) +
                '}';
    }

    public String toStringShort() {
        String dataString = Util.getShortDataString(data);
        return "Block{" +
                "add=" + address +
                ", data=" + dataString +
                '}';
    }
}
