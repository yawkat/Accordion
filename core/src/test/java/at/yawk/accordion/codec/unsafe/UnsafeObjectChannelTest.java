package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.Messenger;
import at.yawk.accordion.codec.ObjectChannel;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
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

        TestTransmB message = new TestTransmB("1", "2", Arrays.asList(0, 1, 2, Integer.MAX_VALUE));

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

        private TestTransmB(String a, String b, List<Integer> c) {
            super(a);
            this.b = b;
            this.c = c;
        }
    }
}