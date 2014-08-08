package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.Channel;
import at.yawk.accordion.Messenger;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;

/**
 * @author yawkat
 */
public class TestMessenger implements Messenger<ByteBuf> {
    @Override
    public Channel<ByteBuf> getChannel(String name) {
        return new Channel<ByteBuf>() {
            @Override
            public void publish(ByteBuf message) {}

            @Override
            public void subscribe(Consumer<ByteBuf> listener) {}
        };
    }
}
