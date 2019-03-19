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

public class BlockStandard implements Block, Serializable {
    private Integer address;
    private byte[] data;

    public BlockStandard() {
    }

    public BlockStandard(int address, byte[] data) {
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
        BlockStandard blockStandard = (BlockStandard) o;
        return getAddress() == blockStandard.getAddress() &&
                Arrays.equals(getData(), blockStandard.getData());
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(getAddress());
        result = 31 * result + Arrays.hashCode(getData());
        return result;
    }

    @Override
    public String toString() {
        return "BlockStandard{" +
                "address=" + address +
                ", data=" + Arrays.toString(data) +
                '}';
    }

    public String toStringShort() {
        String dataString;
        if (data.length > 10) {
            String arrayString = Util.printByteArray(Arrays.copyOf(data, 10), false);
            arrayString = arrayString.substring(0, arrayString.length() - 1);
            arrayString += ", ...";
            dataString = arrayString;
        } else
            dataString = Util.printByteArray(data, false);
        return "Block{" +
                "add=" + address +
                ", data=" + dataString +
                '}';
    }
}
