package oram;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 19-02-2019. <br>
 * Copyright (C) Lind Invest 2019 </p>
 */

public interface AccessStrategy {
    byte[] access(OperationType op, int address, Byte[] data);
}
