package oram;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 19-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class Constants {
    public static final int BLOCK_SIZE = 65536;
    public static final int LOG_OF_BLOCK_SIZE = (int) Math.ceil(Math.log(BLOCK_SIZE) / Math.log(2));
    public static final int DUMMY_BLOCK_ADDRESS = 0;
    public static final int BYTES_OF_RANDOMNESS = 16;
}
