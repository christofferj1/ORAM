package oram.path;

import oram.CommunicationStrategyStub;
import oram.OperationType;
import oram.Util;
import oram.block.BlockEncrypted;
import oram.block.BlockPath;
import oram.encryption.EncryptionStrategy;
import oram.encryption.EncryptionStrategyImpl;
import oram.permutation.PermutationStrategy;
import oram.permutation.PermutationStrategyIdentity;
import oram.util.FactoryStub;
import oram.util.TestUtil;
import org.junit.Before;
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
    private PermutationStrategy permutation;

    @Before
    public void setUp() {
        permutation = new PermutationStrategyIdentity();
    }

    @Test
    public void shouldCalculateTheRightNodeIndexFor7Blocks() {
        byte[] key = "Some key 0".getBytes();
        FactoryStub factoryStub = new FactoryStub(new CommunicationStrategyStub(7, BUCKET_SIZE));
        AccessStrategyPath accessStrategy = new AccessStrategyPath(7, BUCKET_SIZE, key, factoryStub);
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
        AccessStrategyPath accessStrategy = new AccessStrategyPath(15, BUCKET_SIZE, key, factoryStub);
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
        AccessStrategyPath accessStrategy = new AccessStrategyPath(7, BUCKET_SIZE, key, factoryStub);
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
        AccessStrategyPath accessStrategy = new AccessStrategyPath(15, BUCKET_SIZE, key, factoryStub);
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
        AccessStrategyPath accessStrategy = new AccessStrategyPath(7, BUCKET_SIZE, key, factoryStub);
        accessStrategy.setup();

        accessStrategy.access(OperationType.WRITE, 1, "Test 1".getBytes());
        accessStrategy.access(OperationType.WRITE, 4, "Test 2".getBytes());
        accessStrategy.access(OperationType.WRITE, 5, "Test 3".getBytes());
        accessStrategy.access(OperationType.WRITE, 6, "Test 4".getBytes());

        byte[] endObject = accessStrategy.access(OperationType.READ, 1, null);
        assertThat("Value is 'Test 1'", new String(TestUtil.removeTrailingZeroes(endObject)), is("Test 1"));

        endObject = accessStrategy.access(OperationType.READ, 4, null);
        assertThat("Value is 'Test 2'", new String(TestUtil.removeTrailingZeroes(endObject)), is("Test 2"));

        endObject = accessStrategy.access(OperationType.READ, 5, null);
        assertThat("Value is 'Test 3'", new String(TestUtil.removeTrailingZeroes(endObject)), is("Test 3"));

        endObject = accessStrategy.access(OperationType.READ, 6, null);
        assertThat("Value is 'Test 4'", new String(TestUtil.removeTrailingZeroes(endObject)), is("Test 4"));
    }

    @Test
    public void shouldBeAbleToAlterBlocks() {
        byte[] key = "Some key 5".getBytes();
        CommunicationStrategyStub communicationStrategyStub = new CommunicationStrategyStub(15, BUCKET_SIZE);
        FactoryStub factoryStub = new FactoryStub(communicationStrategyStub);
        AccessStrategyPath accessStrategy = new AccessStrategyPath(15, BUCKET_SIZE, key, factoryStub);
        accessStrategy.setup();

        accessStrategy.access(OperationType.WRITE, 4, "Test 1".getBytes());
//        System.out.println("###########################################\n" + server.getTreeString());
        byte[] endObject = accessStrategy.access(OperationType.READ, 4, null);
        assertNotNull(endObject);
        assertThat("Value is 'Test 1'", new String(TestUtil.removeTrailingZeroes(endObject)), is("Test 1"));

//        System.out.println("###########################################\n" + server.getTreeString());
        accessStrategy.access(OperationType.WRITE, 4, "42".getBytes());
//        System.out.println("###########################################\n" + server.getTreeString());
        endObject = accessStrategy.access(OperationType.READ, 4, null);
        assertThat("Value is 42", new String(TestUtil.removeTrailingZeroes(endObject)), is("42"));

//        System.out.println("###########################################\n" + server.getTreeString());
        accessStrategy.access(OperationType.WRITE, 4, "1337".getBytes());
//        System.out.println("###########################################\n" + server.getTreeString());
        endObject = accessStrategy.access(OperationType.READ, 4, null);
        assertThat("Value is 1337", new String(TestUtil.removeTrailingZeroes(endObject)), is("1337"));

//        System.out.println("###########################################\n" + server.getTreeString());
        accessStrategy.access(OperationType.WRITE, 4, "Test 4".getBytes());
//        System.out.println("###########################################\n" + server.getTreeString());
        endObject = accessStrategy.access(OperationType.READ, 4, null);
        assertThat("Value is 'Test 4'", new String(TestUtil.removeTrailingZeroes(endObject)), is("Test 4"));

//        System.out.println("###########################################\n" + server.getTreeString());
    }

    @Test
    public void shouldBeAbleToDecryptAListOfBlocks() {
        byte[] bytes1 = Util.getRandomByteArray(15);
        byte[] bytes2 = Util.getRandomByteArray(16);
        byte[] bytes4 = Util.getRandomByteArray(18);
        BlockPath block0 = new BlockPath(1, bytes1, 0);
        BlockPath block1 = new BlockPath(2, bytes2, 0);
        BlockPath block2 = new BlockPath(0, new byte[17], 0);
        BlockPath block3 = new BlockPath(4, bytes4, 0);

        byte[] key = "Some Key 6".getBytes();
//        Define method specific encryption
        EncryptionStrategy encryptionStrategy = new EncryptionStrategyImpl();
        SecretKey secretKey = encryptionStrategy.generateSecretKey(key);

        BlockEncrypted encrypted0 = new BlockEncrypted(
                encryptionStrategy.encrypt(Util.leIntToByteArray(block0.getAddress()), secretKey),
                encryptionStrategy.encrypt(block0.getData(), secretKey));
        BlockEncrypted encrypted1 = new BlockEncrypted(
                encryptionStrategy.encrypt(Util.leIntToByteArray(block1.getAddress()), secretKey),
                encryptionStrategy.encrypt(block1.getData(), secretKey));
        BlockEncrypted encrypted2 = new BlockEncrypted(
                encryptionStrategy.encrypt(Util.leIntToByteArray(block2.getAddress()), secretKey),
                encryptionStrategy.encrypt(block2.getData(), secretKey));
        BlockEncrypted encrypted3 = new BlockEncrypted(
                encryptionStrategy.encrypt(Util.leIntToByteArray(block3.getAddress()), secretKey),
                encryptionStrategy.encrypt(block3.getData(), secretKey));

        List<BlockEncrypted> encryptedList = Arrays.asList(encrypted0, encrypted1, encrypted2, encrypted3);

        CommunicationStrategyStub communicationStrategyStub = new CommunicationStrategyStub(15, 1);
        FactoryStub factory = new FactoryStub(communicationStrategyStub);
        factory.setEncryptionStrategy(encryptionStrategy);
        AccessStrategyPath access = new AccessStrategyPath(4, 1, key, factory);

        List<BlockPath> res = access.decryptBlockPaths(encryptedList, true);
        assertThat(res, hasSize(3));
        assertThat(res, hasItem(block0));
        assertThat(res, hasItem(block1));
        assertThat(res, hasItem(block3));
    }
}