package oram;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 19-02-2019. <br>
 * Master Thesis 2019 </p>
 */

public class Constants {
    public static final int PORT = 59595;

    public static final byte[] KEY_BYTES = "$ Hello World! $".getBytes();

    public static final int BLOCK_SIZE = 512;
    public static final int DUMMY_BLOCK_ADDRESS = 0;
    public static final int AES_BLOCK_SIZE = 16;
    public static final int AES_KEY_SIZE = 16;
    public static final int INTEGER_BYTE_ARRAY_SIZE = 4;


    public static final int DUMMY_LEAF_NODE_INDEX = -42;

    public static final int POSITION_BLOCK_SIZE = 17;
    public static final int POSITION_MAP_1_SIZE = 64;
    public static final int POSITION_MAP_2_SIZE = 1024;
    public static final int POSITION_MAP_3_SIZE = 16384;
    public static final int POSITION_MAP_4_SIZE = 262144;
}
