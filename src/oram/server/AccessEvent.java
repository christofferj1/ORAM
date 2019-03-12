package oram.server;

import oram.OperationType;

import java.util.List;
import java.util.Objects;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 11-03-2019. <br>
 * Master Thesis 2019 </p>
 */

public class AccessEvent {
    private List<String> addresses;
    private List<byte[]> dataArrays;
    private OperationType operationType;

    public AccessEvent(List<String> addresses, List<byte[]> dataArrays, OperationType operationType) {
        this.addresses = addresses;
        this.dataArrays = dataArrays;
        this.operationType = operationType;
    }

    public List<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }

    public List<byte[]> getDataArrays() {
        return dataArrays;
    }

    public void setDataArrays(List<byte[]> dataArrays) {
        this.dataArrays = dataArrays;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    @Override
    public String toString() {
        return "AccessEvent{" +
                "addresses=" + addresses +
                ", dataArrays=" + dataArrays +
                ", operationType=" + operationType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessEvent that = (AccessEvent) o;
        return Objects.equals(getAddresses(), that.getAddresses()) &&
                Objects.equals(getDataArrays(), that.getDataArrays()) &&
                getOperationType() == that.getOperationType();
    }

    @Override
    public int hashCode() {

        return Objects.hash(getAddresses(), getDataArrays(), getOperationType());
    }
}
