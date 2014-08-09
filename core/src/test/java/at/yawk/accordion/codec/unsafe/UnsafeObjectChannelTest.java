package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.Messenger;
import at.yawk.accordion.codec.ObjectChannel;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnsafeObjectChannelTest {
    @Test
    public void test() {
        Messenger<ByteBuf> messenger = new TestMessenger();

        ObjectChannel<Object> channel = UnsafeObjectChannel.create(messenger);

        HashMap<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        TestTransmB message = new TestTransmB("1", "2", Arrays.asList(0, 1, 2, Integer.MAX_VALUE), map);

        channel.subscribe(TestTransmB.class, s -> assertEquals(message, s));

        channel.publish(message);
    }

    @RequiredArgsConstructor
    @ToString
    @EqualsAndHashCode
    private static class TestTransmA {
        private final String a;
    }

    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    private static class TestTransmB extends TestTransmA {
        private final String b;
        private final List<Integer> c;
        private final Map<String, Integer> d;

        private TestTransmB(String a, String b, List<Integer> c, Map<String, Integer> d) {
            super(a);
            this.b = b;
            this.c = c;
            this.d = d;
        }
    }
}