package at.yawk.accordion.codec.packet;

import java.util.function.Consumer;

/**
 * Channel that allows direct publish-subscribe of packets without manually working with named channels.
 *
 * @author yawkat
 */
public interface PacketChannel {
    /**
     * Publish the given message. Note that this message will also be handled by our subscribers.
     */
    void publish(Packet message);

    /**
     * Listen for the given packet type. Note that subclasses will not call this subscriber. Multiple subscribers for
     * the same packet type may be set, execution order is undefined and not necessarily in registration sequence or the
     * same for all packets (multiple received packets may invoke listeners in different order).
     */
    <P extends Packet> void subscribe(Class<P> clazz, Consumer<P> listener);
}
