package oram.lookahead;

import java.util.Objects;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 11-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class SwapPartnerData {
    private Index index;
    private int swapNumber;

    public SwapPartnerData(Index index, int swapNumber) {
        this.index = index;
        this.swapNumber = swapNumber;
    }

    public Index getIndex() {
        return index;
    }

    public void setIndex(Index index) {
        this.index = index;
    }

    public int getSwapNumber() {
        return swapNumber;
    }

    public void setSwapNumber(int swapNumber) {
        this.swapNumber = swapNumber;
    }

    @Override
    public String toString() {
        return "SwapPartnerData{" +
                "index=" + index +
                ", swapNumber=" + swapNumber +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SwapPartnerData that = (SwapPartnerData) o;
        return getSwapNumber() == that.getSwapNumber() &&
                Objects.equals(getIndex(), that.getIndex());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getIndex(), getSwapNumber());
    }
}
