/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.Channel;
import at.yawk.accordion.Messenger;
import at.yawk.accordion.codec.AbstractObjectChannel;
import at.yawk.accordion.codec.ByteCodec;
import at.yawk.accordion.codec.ObjectChannel;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author yawkat
 */
public class UnsafeObjectChannel extends AbstractObjectChannel<Object> {
    private final CodecSupplier codecs;

    private UnsafeObjectChannel(Messenger<ByteBuf> messenger,
                                Function<Class<?>, String> channelNameFactory,
                                CodecSupplier codecs) {
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
        ((ByteCodec) codecs.getCodecOrThrow(FieldWrapper.clazz(message.getClass())).toByteCodec())
                .encode(target, message);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <P> void listen(Class<P> type, Consumer<P> handler, Channel<ByteBuf> baseChannel) {
        FieldWrapper wrapper = FieldWrapper.clazz(type);
        ByteCodec<P> codec = (ByteCodec<P>) codecs.getCodecOrThrow(wrapper).toByteCodec();
        baseChannel.subscribe(buf -> {
            P message = codec.decode(buf);
            handler.accept(message);
        });
    }
}
