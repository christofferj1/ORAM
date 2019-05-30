package oram.path;

import oram.OperationType;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockPath;
import oram.blockenc.BlockEncryptionStrategyPath;
import oram.clientcom.CommunicationStrategyStub;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyImpl;
import oram.util.FactoryStub;
import oram.util.TestUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;

public class AccessStrategyPathTest {
    private static final int BUCKET_SIZE = 4;

    @Test
    public void shouldCalculateTheRightNodeIndexFor7Blocks() {
        byte[] key = "Some key 0".getBytes();
        FactoryStub factoryStub = new FactoryStub(new CommunicationStrategyStub(7, BUCKET_SIZE));
        AccessStrategyPath accessStrategy = new AccessStrategyPath(7, BUCKET_SIZE, key, factoryStub, null, 0, 0);
        accessStrategy.setup();

        assertThat(accessStrategy.getNode(0, 2), is(3));
        assertThat(accessStrategy.getNode(1, 2), is(4));
        assertThat(accessStrategy.getNode(2, 2), is(5));
        assertThat(accessStrategy.getNode(3, 2), is(6));

        assertThat(accessStrategy.getNode(0, 1), is(1));
        assertThat(accessStrategy.getNode(1, 1), is(1));
        assertThat(accessStrategy.getNode(2, 1), is(2));
        assertThat(accessStrategy.getNode(3, 1), is(2));

        assertThat(accessStrategy.getNode(0, 0), is(0));
        assertThat(accessStrategy.getNode(1, 0), is(0));
        assertThat(accessStrategy.getNode(2, 0), is(0));
        assertThat(accessStrategy.getNode(3, 0), is(0));
    }

    @Test
    public void shouldCalculateTheRightNodeIndexFor15Blocks() {
        byte[] key = "Some key 1".getBytes();
        FactoryStub factoryStub = new FactoryStub(new CommunicationStrategyStub(15, BUCKET_SIZE));
        AccessStrategyPath accessStrategy = new AccessStrategyPath(15, BUCKET_SIZE, key, factoryStub, null, 0, 0);
        accessStrategy.setup();

        assertThat(accessStrategy.getNode(0, 3), is(7));
        assertThat(accessStrategy.getNode(1, 3), is(8));
        assertThat(accessStrategy.getNode(2, 3), is(9));
        assertThat(accessStrategy.getNode(7, 3), is(14));

        assertThat(accessStrategy.getNode(0, 2), is(3));
        assertThat(accessStrategy.getNode(1, 2), is(3));
        assertThat(accessStrategy.getNode(2, 2), is(4));
        assertThat(accessStrategy.getNode(7, 2), is(6));

        assertThat(accessStrategy.getNode(0, 1), is(1));
        assertThat(accessStrategy.getNode(1, 1), is(1));
        assertThat(accessStrategy.getNode(2, 1), is(1));
        assertThat(accessStrategy.getNode(7, 1), is(2));

        assertThat(accessStrategy.getNode(0, 0), is(0));
        assertThat(accessStrategy.getNode(1, 0), is(0));
        assertThat(accessStrategy.getNode(2, 0), is(0));
        assertThat(accessStrategy.getNode(7, 0), is(0));
    }

    @Test
    public void shouldFindTheRightSubTreePositionsSize7() {
        byte[] key = "Some key 2".getBytes();
        FactoryStub factoryStub = new FactoryStub(new CommunicationStrategyStub(7, BUCKET_SIZE));
        AccessStrategyPath accessStrategy = new AccessStrategyPath(7, BUCKET_SIZE, key, factoryStub, null, 0, 0);
        accessStrategy.setup();

        assertThat(accessStrategy.getSubTreeNodes(3), is(Collections.singletonList(0)));
        assertThat(accessStrategy.getSubTreeNodes(4), is(Collections.singletonList(1)));
        assertThat(accessStrategy.getSubTreeNodes(5), is(Collections.singletonList(2)));
        assertThat(accessStrategy.getSubTreeNodes(6), is(Collections.singletonList(3)));

        assertThat(accessStrategy.getSubTreeNodes(1), is(Arrays.asList(0, 1)));
        assertThat(accessStrategy.getSubTreeNodes(2), is(Arrays.asList(2, 3)));

        assertThat(accessStrategy.getSubTreeNodes(0), is(Arrays.asList(0, 1, 2, 3)));
    }

    @Test
    public void shouldFindTheRightSubTreePositionsSize15() {
        byte[] key = "Some key 3".getBytes();
        FactoryStub factoryStub = new FactoryStub(new CommunicationStrategyStub(15, BUCKET_SIZE));
        AccessStrategyPath accessStrategy = new AccessStrategyPath(15, BUCKET_SIZE, key, factoryStub, null, 0, 0);
        accessStrategy.setup();

        assertThat(accessStrategy.getSubTreeNodes(7), is(Collections.singletonList(0)));
        assertThat(accessStrategy.getSubTreeNodes(8), is(Collections.singletonList(1)));
        assertThat(accessStrategy.getSubTreeNodes(9), is(Collections.singletonList(2)));
        assertThat(accessStrategy.getSubTreeNodes(10), is(Collections.singletonList(3)));
        assertThat(accessStrategy.getSubTreeNodes(11), is(Collections.singletonList(4)));
        assertThat(accessStrategy.getSubTreeNodes(12), is(Collections.singletonList(5)));
        assertThat(accessStrategy.getSubTreeNodes(13), is(Collections.singletonList(6)));
        assertThat(accessStrategy.getSubTreeNodes(14), is(Collections.singletonList(7)));

        assertThat(accessStrategy.getSubTreeNodes(3), is(Arrays.asList(0, 1)));
        assertThat(accessStrategy.getSubTreeNodes(4), is(Arrays.asList(2, 3)));
        assertThat(accessStrategy.getSubTreeNodes(5), is(Arrays.asList(4, 5)));
        assertThat(accessStrategy.getSubTreeNodes(6), is(Arrays.asList(6, 7)));

        assertThat(accessStrategy.getSubTreeNodes(1), is(Arrays.asList(0, 1, 2, 3)));
        assertThat(accessStrategy.getSubTreeNodes(2), is(Arrays.asList(4, 5, 6, 7)));

        assertThat(accessStrategy.getSubTreeNodes(0), is(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7)));
    }

    @Test
    public void shouldBeAbleToFillInBlocks() {
        byte[] key = "Some key 4".getBytes();
        FactoryStub factoryStub = new FactoryStub(new CommunicationStrategyStub(7, BUCKET_SIZE));
        AccessStrategyPath accessStrategy = new AccessStrategyPath(7, BUCKET_SIZE, key, factoryStub, null, 0, 0);
        accessStrategy.setup();

        accessStrategy.access(OperationType.WRITE, 1, "Test 1".getBytes(), false, false);
        accessStrategy.access(OperationType.WRITE, 4, "Test 2".getBytes(), false, false);
        accessStrategy.access(OperationType.WRITE, 5, "Test 3".getBytes(), false, false);
        accessStrategy.access(OperationType.WRITE, 6, "Test 4".getBytes(), false, false);

        byte[] endObject = accessStrategy.access(OperationType.READ, 1, null, false, false);
        assertThat("Value is 'Test 1'", new String(TestUtil.removeTrailingZeroes(endObject)), is("Test 1"));

        endObject = accessStrategy.access(OperationType.READ, 4, null, false, false);
        assertThat("Value is 'Test 2'", new String(TestUtil.removeTrailingZeroes(endObject)), is("Test 2"));

        endObject = accessStrategy.access(OperationType.READ, 5, null, false, false);
        assertThat("Value is 'Test 3'", new String(TestUtil.removeTrailingZeroes(endObject)), is("Test 3"));

        endObject = accessStrategy.access(OperationType.READ, 6, null, false, false);
        assertThat("Value is 'Test 4'", new String(TestUtil.removeTrailingZeroes(endObject)), is("Test 4"));
    }

    @Test
    public void shouldBeAbleToAlterBlocks() {
        byte[] key = "Some key 5".getBytes();
        CommunicationStrategyStub communicationStrategyStub = new CommunicationStrategyStub(15, BUCKET_SIZE);
        FactoryStub factoryStub = new FactoryStub(communicationStrategyStub);
        AccessStrategyPath accessStrategy = new AccessStrategyPath(15, BUCKET_SIZE, key, factoryStub, null, 0, 0);
        accessStrategy.setup();

        accessStrategy.access(OperationType.WRITE, 4, "Test 1".getBytes(), false, false);
        byte[] endObject = accessStrategy.access(OperationType.READ, 4, null, false, false);
        assertNotNull(endObject);
        assertThat("Value is 'Test 1'", new String(TestUtil.removeTrailingZeroes(endObject)), is("Test 1"));

        accessStrategy.access(OperationType.WRITE, 4, "42".getBytes(), false, false);
        endObject = accessStrategy.access(OperationType.READ, 4, null, false, false);
        assertThat("Value is 42", new String(TestUtil.removeTrailingZeroes(endObject)), is("42"));

        accessStrategy.access(OperationType.WRITE, 4, "1337".getBytes(), false, false);
        endObject = accessStrategy.access(OperationType.READ, 4, null, false, false);
        assertThat("Value is 1337", new String(TestUtil.removeTrailingZeroes(endObject)), is("1337"));

        accessStrategy.access(OperationType.WRITE, 4, "Test 4".getBytes(), false, false);
        endObject = accessStrategy.access(OperationType.READ, 4, null, false, false);
        assertThat("Value is 'Test 4'", new String(TestUtil.removeTrailingZeroes(endObject)), is("Test 4"));
    }

    @Test
    public void shouldBeAbleToDecryptAListOfBlocks() {
        byte[] bytes1 = Util.getRandomByteArray(15);
        byte[] bytes2 = Util.getRandomByteArray(16);
        byte[] bytes4 = Util.getRandomByteArray(18);
        int index1 = 0;
        int index2 = 1;
        int index3 = 0;
        int index4 = 3;
        BlockPath block0 = new BlockPath(1, bytes1, index1);
        BlockPath block1 = new BlockPath(2, bytes2, index2);
        BlockPath block2 = new BlockPath(0, new byte[17], index3);
        BlockPath block3 = new BlockPath(4, bytes4, index4);

        byte[] key = "Some Key 6".getBytes();
//        Define method specific encryption
        EncryptionStrategy encryptionStrategy = new EncryptionStrategyImpl();
        SecretKey secretKey = encryptionStrategy.generateSecretKey(key);

        byte[] data0 = block0.getData();
        byte[] datac0 = encryptionStrategy.encrypt(data0, secretKey);
        byte[] indexb0 = Util.leIntToByteArray(block0.getIndex());
        byte[] indexc0 = encryptionStrategy.encrypt(indexb0, secretKey);
        BlockEncrypted encrypted0 = new BlockEncrypted(
                encryptionStrategy.encrypt(Util.leIntToByteArray(block0.getAddress()), secretKey),
                ArrayUtils.addAll(datac0,
                        indexc0));

        System.out.println("Block 1");
        byte[] data1 = block1.getData();
        byte[] datac1 = encryptionStrategy.encrypt(data1, secretKey);
        byte[] indexb1 = Util.leIntToByteArray(block1.getIndex());
        byte[] indexc1 = encryptionStrategy.encrypt(indexb1, secretKey);
        BlockEncrypted encrypted1 = new BlockEncrypted(
                encryptionStrategy.encrypt(Util.leIntToByteArray(block1.getAddress()), secretKey),
                ArrayUtils.addAll(datac1,
                        indexc1));

        System.out.println("Block 2");
        byte[] data2 = block2.getData();
        byte[] datac2 = encryptionStrategy.encrypt(data2, secretKey);
        byte[] indexb2 = Util.leIntToByteArray(block2.getIndex());
        byte[] indexc2 = encryptionStrategy.encrypt(indexb2, secretKey);
        BlockEncrypted encrypted2 = new BlockEncrypted(
                encryptionStrategy.encrypt(Util.leIntToByteArray(block2.getAddress()), secretKey),
                ArrayUtils.addAll(datac2,
                        indexc2));

        byte[] data3 = block3.getData();
        byte[] datac3 = encryptionStrategy.encrypt(data3, secretKey);
        byte[] indexb3 = Util.leIntToByteArray(block3.getIndex());
        byte[] indexc3 = encryptionStrategy.encrypt(indexb3, secretKey);
        BlockEncrypted encrypted3 = new BlockEncrypted(
                encryptionStrategy.encrypt(Util.leIntToByteArray(block3.getAddress()), secretKey),
                ArrayUtils.addAll(datac3,
                        indexc3));

        System.out.println("Block 0");
        System.out.println("    Data 0 " + Arrays.toString(data0));
        System.out.println("    Data c 0 " + Arrays.toString(datac0));
        System.out.println("    Index b 0 " + Arrays.toString(indexb0));
        System.out.println("    Index c 0 " + Arrays.toString(indexc0));

        System.out.println("Block 1");
        System.out.println("    Data 1 " + Arrays.toString(data1));
        System.out.println("    Data c 1 " + Arrays.toString(datac1));
        System.out.println("    Index b 1 " + Arrays.toString(indexb1));
        System.out.println("    Index c 1 " + Arrays.toString(indexc1));

        System.out.println("Block 2");
        System.out.println("    Data 2 " + Arrays.toString(data2));
        System.out.println("    Data c 2 " + Arrays.toString(datac2));
        System.out.println("    Index b 2 " + Arrays.toString(indexb2));
        System.out.println("    Index c 2 " + Arrays.toString(indexc2));

        System.out.println("Block 3");
        System.out.println("    Data 3 " + Arrays.toString(data3));
        System.out.println("    Data c 3 " + Arrays.toString(datac3));
        System.out.println("    Index b 3 " + Arrays.toString(indexb3));
        System.out.println("    Index c 3 " + Arrays.toString(indexc3));

        List<BlockEncrypted> encryptedList = Arrays.asList(encrypted0, encrypted1, encrypted2, encrypted3);

        CommunicationStrategyStub communicationStrategyStub = new CommunicationStrategyStub(15, 1);
        FactoryStub factory = new FactoryStub(communicationStrategyStub);
        factory.setEncryptionStrategy(encryptionStrategy);

        BlockEncryptionStrategyPath blockEncryptionStrategyPath =
                new BlockEncryptionStrategyPath(factory.getEncryptionStrategy(), factory.getPermutationStrategy());
        List<BlockPath> res = blockEncryptionStrategyPath.decryptBlocks(encryptedList, secretKey, true);
        assertThat(res, hasSize(3));
        assertThat(res, hasItem(block0));
        assertThat(res, hasItem(block1));
        assertThat(res, hasItem(block3));
    }
}