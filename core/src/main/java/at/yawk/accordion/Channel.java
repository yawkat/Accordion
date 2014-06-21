package at.yawk.accordion;

import at.yawk.accordion.codec.Codec;
import java.util.function.Consumer;

/**
 * Channel with publish-subscribe support of type T.
 *
 * @author yawkat
 */
public interface Channel<T> {
    /**
     * Publish the message to this channel.
     */
    void publish(T message);

    /**
     * Subscribe to this channel.
     */
    void subscribe(Consumer<T> listener);

    /**
     * Transform this channel to another type using the given codec.
     */
    default <U> Channel<U> decode(Codec<T, U> codec) {
        Channel<T> original = this;
        return new Channel<U>() {
            @Override
            public void publish(U message) {
                original.publish(codec.encode(message));
            }

            @Override
            public void subscribe(Consumer<U> listener) {
                original.subscribe(message -> listener.accept(codec.decode(message)));
            }
        };
    }
}
