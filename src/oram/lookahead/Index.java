package oram.lookahead;

import java.util.Objects;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 09-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class Index {
    private int rowIndex;
    private int colIndex;

    public Index(int rowIndex, int colIndex) {
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public int getColIndex() {
        return colIndex;
    }

    @Override
    public String toString() {
        return "Index{" +
                "rowIndex=" + rowIndex +
                ", colIndex=" + colIndex +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Index index = (Index) o;
        return getRowIndex() == index.getRowIndex() &&
                getColIndex() == index.getColIndex();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRowIndex(), getColIndex());
    }
}
