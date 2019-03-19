package oram.block;

import oram.Util;
import oram.lookahead.Index;

import java.util.Arrays;
import java.util.Objects;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 04-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class BlockLookahead implements Block {
    private int address;
    private byte[] data;
    private int colIndex;
    private int rowIndex;

    public BlockLookahead() {
    }

    public BlockLookahead(int address, byte[] data) {
        this.address = address;
        this.data = data;
    }

    public BlockLookahead(int address, byte[] data, int rowIndex, int colIndex) {
        this.address = address;
        this.data = data;
        this.colIndex = colIndex;
        this.rowIndex = rowIndex;
    }

    public Index getIndex() {
        return new Index(rowIndex, colIndex);
    }

    public void setIndex(Index index) {
        rowIndex = index.getRowIndex();
        colIndex = index.getColIndex();
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

    public int getColIndex() {
        return colIndex;
    }

    public void setColIndex(int colIndex) {
        this.colIndex = colIndex;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    @Override
    public String toString() {
        return "Lookahead{" +
                "add=" + (address < 10 ? " " + address : address) +
                ", data=" + Util.printByteArray(data, false) +
                ", row=" + rowIndex +
                ", col=" + colIndex +
                '}';
    }

    public String toStringShort() {
        String dataString;
        if (data.length > 10)
            dataString = Util.printByteArray(Arrays.copyOf(data, 10), false);
        else
            dataString = Util.printByteArray(data, false);
        return "Lookahead{" +
                "add=" + (address < 10 ? " " + address : address) +
                ", (" + rowIndex + ", " + colIndex + ")" +
                 ", data=" + dataString +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockLookahead that = (BlockLookahead) o;
        return getAddress() == that.getAddress() &&
                getColIndex() == that.getColIndex() &&
                getRowIndex() == that.getRowIndex() &&
                Arrays.equals(getData(), that.getData());
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(getAddress(), getColIndex(), getRowIndex());
        result = 31 * result + Arrays.hashCode(getData());
        return result;
    }
}
