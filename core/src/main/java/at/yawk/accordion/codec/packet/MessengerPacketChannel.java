package at.yawk.accordion.codec.packet;

import at.yawk.accordion.Messenger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * PacketChannel implementation that can write to a ByteBuf Messenger.
 *
 * @author yawkat
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MessengerPacketChannel implements PacketChannel {
    private final Messenger<ByteBuf> messenger;

    /**
     * The function used to generate a unique channel name for a packet class.
     */
    private final Function<Class<?>, String> channelNameFactory;

    private final Map<Class<? extends Packet>, Collection<Consumer<? extends Packet>>> subscribers
            = new ConcurrentHashMap<>();

    /**
     * Create a new PacketChannel for the given messenger using the default channel name factory. This factory might
     * change with versions.
     */
    public static PacketChannel create(Messenger<ByteBuf> messenger) {
        // use class name as channel name
        return create(messenger, Class::getName);
    }

    /**
     * Create a new PacketChannel for the given messenger that uses the channelNameFactory to generate channel names for
     * packet classes.
     */
    public static PacketChannel create(Messenger<ByteBuf> messenger, Function<Class<?>, String> channelNameFactory) {
        return new MessengerPacketChannel(messenger, channelNameFactory);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void publish(Packet message) {
        // get the channel name
        Class<? extends Packet> clazz = message.getClass();
        String channel = channelNameFactory.apply(clazz);
        // write packet to empty buffer
        ByteBuf payload = Unpooled.buffer();
        message.write(payload);
        // send buffer to channel of this packet type
        messenger.getChannel(channel).publish(payload);

        // send to our subscribers to follow PacketChannel contract
        subscribers.getOrDefault(clazz, Collections.emptySet())
                .forEach((Consumer subscriber) -> subscriber.accept(message));
    }

    @Override
    public <P extends Packet> void subscribe(Class<P> clazz, Consumer<P> listener) {
        try {
            // use empty constructor
            Constructor<P> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            // subscribe
            messenger.getChannel(channelNameFactory.apply(clazz)).subscribe(buf -> {
                try {
                    // create the packet
                    P packet = constructor.newInstance();
                    // read the packet's contents
                    packet.read(buf);
                    // handle the packet
                    listener.accept(packet);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    // this should be counted as a packet read error
                    throw new RuntimeException("Error while initializing packet", e);
                }
            });

            // add to subscribers map
            subscribers.computeIfAbsent(clazz, cl -> new CopyOnWriteArrayList<>()).add(listener);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Packets need an empty constructor to be used for messaging!", e);
        }
    }
}
