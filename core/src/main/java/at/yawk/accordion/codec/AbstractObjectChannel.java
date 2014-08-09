package at.yawk.accordion.codec;

import at.yawk.accordion.Channel;
import at.yawk.accordion.Messenger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
 * @author yawkat
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractObjectChannel<T> implements ObjectChannel<T> {
    private final Messenger<ByteBuf> messenger;

    /**
     * The function used to generate a unique channel name for a packet class.
     */
    private final Function<Class<?>, String> channelNameFactory;

    private final Map<Class<? extends T>, Collection<Consumer<? extends T>>> subscribers
            = new ConcurrentHashMap<>();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void publish(T message) {
        // get the channel name
        Class<? extends T> clazz = (Class) message.getClass();
        String channel = channelNameFactory.apply(clazz);
        // write packet to empty buffer
        ByteBuf payload = Unpooled.buffer();
        write(message, payload);
        // send buffer to channel of this packet type
        messenger.getChannel(channel).publish(payload);

        // send to our subscribers to follow PacketChannel contract
        subscribers.getOrDefault(clazz, Collections.emptySet())
                .forEach((Consumer subscriber) -> subscriber.accept(message));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <P extends T> void subscribe(Class<P> clazz, Consumer<P> listener) {
        // add to subscribers map
        Collection<Consumer<P>> subs = (Collection) subscribers.computeIfAbsent(clazz,
                                                                                cl -> new CopyOnWriteArrayList<>());
        if (subs.isEmpty()) {
            listen(clazz, p -> subs.forEach(l -> l.accept(p)), messenger.getChannel(channelNameFactory.apply(clazz)));
        }
        subs.add(listener);
    }

    protected abstract void write(T message, ByteBuf target);

    protected abstract <P extends T> void listen(Class<P> type, Consumer<P> handler, Channel<ByteBuf> baseChannel);
}
