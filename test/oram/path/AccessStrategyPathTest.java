package oram.path;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AccessStrategyPathTest {

    @Test
    public void shouldCalculateTheRightNodeIndexFor7Blocks() {
        Client client = new Client(7, new Server(7), 4, "Some key 0");

        assertThat(client.getPosition(0, 2), is(3));
        assertThat(client.getPosition(1, 2), is(4));
        assertThat(client.getPosition(2, 2), is(5));
        assertThat(client.getPosition(3, 2), is(6));

        assertThat(client.getPosition(0, 1), is(1));
        assertThat(client.getPosition(1, 1), is(1));
        assertThat(client.getPosition(2, 1), is(2));
        assertThat(client.getPosition(3, 1), is(2));

        assertThat(client.getPosition(0, 0), is(0));
        assertThat(client.getPosition(1, 0), is(0));
        assertThat(client.getPosition(2, 0), is(0));
        assertThat(client.getPosition(3, 0), is(0));
    }


    @Test
    public void shouldCalculateTheRightNodeIndexFor15Blocks() {
        Client client = new Client(15, new Server(15), 4, "Some key 1");

        assertThat(client.getPosition(0, 3), is(7));
        assertThat(client.getPosition(1, 3), is(8));
        assertThat(client.getPosition(2, 3), is(9));
        assertThat(client.getPosition(7, 3), is(14));

        assertThat(client.getPosition(0, 2), is(3));
        assertThat(client.getPosition(1, 2), is(3));
        assertThat(client.getPosition(2, 2), is(4));
        assertThat(client.getPosition(7, 2), is(6));

        assertThat(client.getPosition(0, 1), is(1));
        assertThat(client.getPosition(1, 1), is(1));
        assertThat(client.getPosition(2, 1), is(1));
        assertThat(client.getPosition(7, 1), is(2));

        assertThat(client.getPosition(0, 0), is(0));
        assertThat(client.getPosition(1, 0), is(0));
        assertThat(client.getPosition(2, 0), is(0));
        assertThat(client.getPosition(7, 0), is(0));
    }

    @Test
    public void shouldFindTheRightSubTreePositionsSize7() {
        Client client = new Client(7, new Server(7), 4, "Some key 2");
        assertThat(client.getSubTreeNodes(3), is(Collections.singletonList(0)));
        assertThat(client.getSubTreeNodes(4), is(Collections.singletonList(1)));
        assertThat(client.getSubTreeNodes(5), is(Collections.singletonList(2)));
        assertThat(client.getSubTreeNodes(6), is(Collections.singletonList(3)));

        assertThat(client.getSubTreeNodes(1), is(Arrays.asList(0, 1)));
        assertThat(client.getSubTreeNodes(2), is(Arrays.asList(2, 3)));

        assertThat(client.getSubTreeNodes(0), is(Arrays.asList(0, 1, 2, 3)));
    }

    @Test
    public void shouldFindTheRightSubTreePositionsSize15() {
        Client client = new Client(15, new Server(15), 4, "Some key 3");

        assertThat(client.getSubTreeNodes(7), is(Collections.singletonList(0)));
        assertThat(client.getSubTreeNodes(8), is(Collections.singletonList(1)));
        assertThat(client.getSubTreeNodes(9), is(Collections.singletonList(2)));
        assertThat(client.getSubTreeNodes(10), is(Collections.singletonList(3)));
        assertThat(client.getSubTreeNodes(11), is(Collections.singletonList(4)));
        assertThat(client.getSubTreeNodes(12), is(Collections.singletonList(5)));
        assertThat(client.getSubTreeNodes(13), is(Collections.singletonList(6)));
        assertThat(client.getSubTreeNodes(14), is(Collections.singletonList(7)));

        assertThat(client.getSubTreeNodes(3), is(Arrays.asList(0, 1)));
        assertThat(client.getSubTreeNodes(4), is(Arrays.asList(2, 3)));
        assertThat(client.getSubTreeNodes(5), is(Arrays.asList(4, 5)));
        assertThat(client.getSubTreeNodes(6), is(Arrays.asList(6, 7)));

        assertThat(client.getSubTreeNodes(1), is(Arrays.asList(0, 1, 2, 3)));
        assertThat(client.getSubTreeNodes(2), is(Arrays.asList(4, 5, 6, 7)));

        assertThat(client.getSubTreeNodes(0), is(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7)));
    }

    @Test
    public void shouldBeAbleToFillInBlocks() {
        Server server = new Server(7);
        Client client = new Client(7, server, 4, "Some key 4");

        client.access(AccessType.WRITE, "0", "Test 1");
        client.access(AccessType.WRITE, "4", "Test 2");
        client.access(AccessType.WRITE, "5", "Test 3");
        client.access(AccessType.WRITE, "6", "Test 4");

        String endObject = client.access(AccessType.READ, "0000000", null);
        assertThat("Value is 'Test 1'", endObject, is("Test 1"));

        endObject = client.access(AccessType.READ, "0000004", null);
        assertThat("Value is 'Test 2'", endObject, is("Test 2"));

        endObject = client.access(AccessType.READ, "0000005", null);
        assertThat("Value is 'Test 3'", endObject, is("Test 3"));

        endObject = client.access(AccessType.READ, "0000006", null);
        assertThat("Value is 'Test 4'", endObject, is("Test 4"));
    }

    @Test
    public void shouldBeAbleToAlterBlocks() {
        Server server = new Server(15);
        Client client = new Client(15, server, 4, "Some key 5");

        client.access(AccessType.WRITE, "0000004", "Test 1");
        String endObject = client.access(AccessType.READ, "0000004", null);
        assertThat("Value is 'Test 1'", endObject, is("Test 1"));

        client.access(AccessType.WRITE, "0000004", "42");
        endObject = client.access(AccessType.READ, "0000004", null);
        assertThat("Value is 42d", endObject, is("42"));

        client.access(AccessType.WRITE, "0000004", "1337");
        endObject = client.access(AccessType.READ, "0000004", null);
        assertThat("Value is 1337", endObject, is("1337"));

        client.access(AccessType.WRITE, "0000004", "Test 4");
        endObject = client.access(AccessType.READ, "0000004", null);
        assertThat("Value is 'Test 4'", endObject, is("Test 4"));
    }
}