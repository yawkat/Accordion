package at.yawk.accordion.codec.packet;

import at.yawk.accordion.Channel;
import at.yawk.accordion.Messenger;
import at.yawk.accordion.codec.AbstractObjectChannel;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * PacketChannel implementation that can write to a ByteBuf Messenger.
 *
 * @author yawkat
 */
public class MessengerPacketChannel extends AbstractObjectChannel<Packet> implements PacketChannel {
    private MessengerPacketChannel(Messenger<ByteBuf> messenger, Function<Class<?>, String> channelNameFactory) {
        super(messenger, channelNameFactory);
    }

    /**
     * Create a new PacketChannel for the given messenger using the default channel name factory. This factory might
     * change with versions.
     */
    public static PacketChannel create(Messenger<ByteBuf> messenger) {
        // use class name as channel name
        return create(messenger, Class::getName);
    }

    /**
     * Create a new PacketChannel for the given messenger that uses the channelNameFactory to generate channel names
     * for
     * packet classes.
     */
    public static PacketChannel create(Messenger<ByteBuf> messenger, Function<Class<?>, String> channelNameFactory) {
        return new MessengerPacketChannel(messenger, channelNameFactory);
    }

    @Override
    protected void write(Packet message, ByteBuf target) {
        message.write(target);
    }

    @Override
    protected <P extends Packet> void listen(Class<P> type, Consumer<P> handler, Channel<ByteBuf> baseChannel) {
        // use empty constructor
        Constructor<P> constructor;
        try {
            constructor = type.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Packets need an empty constructor to be used for messaging!", e);
        }
        constructor.setAccessible(true);
        // subscribe
        baseChannel.subscribe(buf -> {
            try {
                // create the packet
                P packet = constructor.newInstance();
                // read the packet's contents
                packet.read(buf);
                // handle the packet
                handler.accept(packet);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                // this should be counted as a packet read error
                throw new RuntimeException("Error while initializing packet", e);
            }
        });
    }
}
