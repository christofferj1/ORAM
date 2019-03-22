package oram.block;

import oram.Util;

import java.io.Serializable;
import java.util.Arrays;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 23-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class BlockEncrypted implements Serializable {
    private byte[] address;
    private byte[] data;

    public BlockEncrypted(byte[] address, byte[] data) {
        this.address = address;
        this.data = data;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
//        if (Util.isDummyAddress(Util.byteArrayToLeInt(address)))
//            return "DUMMY COUNTER: " + Util.byteArrayToLeInt(data);
        return "BlockEncrypted{" +
                "address=" + Util.printByteArray(address, true) + // TODO: should not trim
                ", data=" + Util.printByteArray(data, true) + // TODO: should not trim
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
                "add=" + Util.printByteArray(address, true) +
                ", data=" + dataString +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockEncrypted that = (BlockEncrypted) o;
        return Arrays.equals(getAddress(), that.getAddress()) &&
                Arrays.equals(getData(), that.getData());
    }

    @Override
    public int hashCode() {

        int result = Arrays.hashCode(getAddress());
        result = 31 * result + Arrays.hashCode(getData());
        return result;
    }
}