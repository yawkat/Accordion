package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.Channel;
import at.yawk.accordion.Messenger;
import at.yawk.accordion.codec.AbstractObjectChannel;
import at.yawk.accordion.codec.ByteCodec;
import at.yawk.accordion.codec.ObjectChannel;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author yawkat
 */
public class UnsafeObjectChannel extends AbstractObjectChannel<Object> {
    private final CodecManager codecs;
    private final Map<Class<?>, ClassData<?>> schemas = new ConcurrentHashMap<>();

    private UnsafeObjectChannel(Messenger<ByteBuf> messenger,
                                Function<Class<?>, String> channelNameFactory,
                                CodecManager codecs) {
        super(messenger, channelNameFactory);
        this.codecs = codecs;
    }

    /**
     * Create a new PacketChannel for the given messenger using the default channel name factory. This factory might
     * change with versions.
     */
    public static ObjectChannel<Object> create(Messenger<ByteBuf> messenger) {
        // use class name as channel name
        return create(messenger, Class::getName);
    }

    /**
     * Create a new PacketChannel for the given messenger that uses the channelNameFactory to generate channel names
     * for packet classes.
     */
    public static ObjectChannel<Object> create(Messenger<ByteBuf> messenger,
                                               Function<Class<?>, String> channelNameFactory) {
        return create(messenger, channelNameFactory, CodecManager.getDefaultManager());
    }

    /**
     * Create a new PacketChannel for the given messenger that uses the channelNameFactory to generate channel names
     * for packet classes.
     */
    public static ObjectChannel<Object> create(Messenger<ByteBuf> messenger,
                                               Function<Class<?>, String> channelNameFactory,
                                               CodecManager codecs) {
        return new UnsafeObjectChannel(messenger, channelNameFactory, codecs);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void write(Object message, ByteBuf target) {
        ((ClassData) getSchema(message.getClass())).getCodec().encode(target, message);
    }

    @Override
    protected <P> void listen(Class<P> type, Consumer<P> handler, Channel<ByteBuf> baseChannel) {
        ClassData<P> schema = getSchema(type);
        synchronized (schema.getListeners()) {
            if (schema.getListeners().isEmpty()) {
                baseChannel.subscribe(buf -> {
                    P m = schema.getCodec().decode(buf);
                    schema.getListeners().forEach(l -> l.accept(m));
                });
            }
            schema.getListeners().add(handler);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> ClassData<T> getSchema(Class<T> type) {
        return (ClassData<T>) schemas
                .computeIfAbsent(type, t -> new ClassData((ByteCodec) codecs.findFirstLevelCodec(t)));
    }
}
