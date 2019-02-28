package oram.path;

import oram.OperationType;
import oram.server.Server;
import oram.util.ServerStub;
import oram.util.TestUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AccessStrategyPathTest {
    private static final int BUCKET_SIZE = 4;

    @Test
    public void shouldCalculateTheRightNodeIndexFor7Blocks() {
        String key = "Some key 0";
        AccessStrategyPath accessStrategy = new AccessStrategyPath(7, new ServerStub(7, BUCKET_SIZE), 4, key);

        assertThat(accessStrategy.getPosition(0, 2), is(12));
        assertThat(accessStrategy.getPosition(1, 2), is(16));
        assertThat(accessStrategy.getPosition(2, 2), is(20));
        assertThat(accessStrategy.getPosition(3, 2), is(24));

        assertThat(accessStrategy.getPosition(0, 1), is(4));
        assertThat(accessStrategy.getPosition(1, 1), is(4));
        assertThat(accessStrategy.getPosition(2, 1), is(8));
        assertThat(accessStrategy.getPosition(3, 1), is(8));

        assertThat(accessStrategy.getPosition(0, 0), is(0));
        assertThat(accessStrategy.getPosition(1, 0), is(0));
        assertThat(accessStrategy.getPosition(2, 0), is(0));
        assertThat(accessStrategy.getPosition(3, 0), is(0));
    }


    @Test
    public void shouldCalculateTheRightNodeIndexFor15Blocks() {
        String key = "Some key 1";
        AccessStrategyPath accessStrategy = new AccessStrategyPath(15, new ServerStub(15,BUCKET_SIZE), 4, key);

        assertThat(accessStrategy.getPosition(0, 3), is(28));
        assertThat(accessStrategy.getPosition(1, 3), is(32));
        assertThat(accessStrategy.getPosition(2, 3), is(36));
        assertThat(accessStrategy.getPosition(7, 3), is(56));

        assertThat(accessStrategy.getPosition(0, 2), is(12));
        assertThat(accessStrategy.getPosition(1, 2), is(12));
        assertThat(accessStrategy.getPosition(2, 2), is(16));
        assertThat(accessStrategy.getPosition(7, 2), is(24));

        assertThat(accessStrategy.getPosition(0, 1), is(4));
        assertThat(accessStrategy.getPosition(1, 1), is(4));
        assertThat(accessStrategy.getPosition(2, 1), is(4));
        assertThat(accessStrategy.getPosition(7, 1), is(8));

        assertThat(accessStrategy.getPosition(0, 0), is(0));
        assertThat(accessStrategy.getPosition(1, 0), is(0));
        assertThat(accessStrategy.getPosition(2, 0), is(0));
        assertThat(accessStrategy.getPosition(7, 0), is(0));
    }

    @Test
    public void shouldFindTheRightSubTreePositionsSize7() {
        String key = "Some key 2";
        AccessStrategyPath accessStrategy = new AccessStrategyPath(7, new ServerStub(7, BUCKET_SIZE), 4, key);
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
        String key = "Some key 3";
        AccessStrategyPath accessStrategy = new AccessStrategyPath(15, new ServerStub(15, BUCKET_SIZE), 4, key);

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
        String key = "Some key 4";
        Server server = new ServerStub(7, BUCKET_SIZE);
        AccessStrategyPath accessStrategy = new AccessStrategyPath(7, server, 4, key);

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
        String key = "Some key 5";
        Server server = new ServerStub(15, BUCKET_SIZE);
        AccessStrategyPath accessStrategy = new AccessStrategyPath(15, server, 4, key);

        accessStrategy.access(OperationType.WRITE, 4, "Test 1".getBytes());
        byte[] endObject = accessStrategy.access(OperationType.READ, 4, null);
        assertThat("Value is 'Test 1'", new String(TestUtil.removeTrailingZeroes(endObject)), is("Test 1"));

        accessStrategy.access(OperationType.WRITE, 4, "42".getBytes());
        endObject = accessStrategy.access(OperationType.READ, 4, null);
        assertThat("Value is 42", new String(TestUtil.removeTrailingZeroes(endObject)), is("42"));

        accessStrategy.access(OperationType.WRITE, 4, "1337".getBytes());
        endObject = accessStrategy.access(OperationType.READ, 4, null);
        assertThat("Value is 1337", new String(TestUtil.removeTrailingZeroes(endObject)), is("1337"));

        accessStrategy.access(OperationType.WRITE, 4, "Test 4".getBytes());
        endObject = accessStrategy.access(OperationType.READ, 4, null);
        assertThat("Value is 'Test 4'", new String(TestUtil.removeTrailingZeroes(endObject)), is("Test 4"));
    }
}