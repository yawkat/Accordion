package at.yawk.accordion;

import at.yawk.accordion.codec.Codec;

/**
 * Messenger that provides unique channels by name.
 *
 * @author yawkat
 */
@FunctionalInterface
public interface Messenger<T> {
    Channel<T> getChannel(String name);

    /**
     * Transform this channel using the given codec.
     */
    default <U> Messenger<U> decode(Codec<T, U> codec) {
        return name -> getChannel(name).decode(codec);
    }
}
