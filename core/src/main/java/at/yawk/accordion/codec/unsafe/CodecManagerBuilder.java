package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

/**
 * @author yawkat
 */
public class CodecManagerBuilder {
    private final Map<Class<?>, CodecFactory<?>> codecs = new HashMap<>();

    private CodecManagerBuilder() {
        appendDefaults();
    }

    public static CodecManagerBuilder create() {
        return new CodecManagerBuilder();
    }

    private <T> CodecManagerBuilder appendFactory(Class<T> clazz, CodecFactory<T> codecFactory) {
        codecs.put(clazz, codecFactory);
        return this;
    }

    private <T> CodecManagerBuilder appendUnsafe(Class<T> clazz, UnsafeCodec<T> codec) {
        return appendFactory(clazz, (m, f) -> codec);
    }

    public <T> CodecManagerBuilder append(Class<T> clazz, ByteCodec<T> codec) {
        return appendUnsafe(clazz, new UnsafeByteCodec<T>(codec));
    }

    private <T> CodecManagerBuilder appendByte(Class<T> clazz,
                                               Function<ByteBuf, T> reader,
                                               BiConsumer<ByteBuf, T> writer) {
        return appendUnsafe(clazz, new UnsafeCodec<T>() {
            @Override
            public void encode(ByteBuf target, T message) {
                writer.accept(target, message);
            }

            @Override
            public T decode(ByteBuf encoded) {
                return reader.apply(encoded);
            }
        });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void append(Class<?> clazz, UnsafeReader reader, UnsafeWriter writer) {
        appendUnsafe(clazz, (UnsafeCodec) UnsafeCodec.build(reader, writer));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <C extends Collection<Object>> void appendCollection(Class<C> type, IntFunction<? extends C> factory) {
        appendFactory(type, CollectionCodec.forType(factory));
        Stream.concat(Stream.of(type.getSuperclass()), Arrays.stream(type.getInterfaces()))
                .filter(sup -> sup != null)
                .filter(Collection.class::isAssignableFrom)
                .forEach(sup -> appendCollection((Class) sup, factory));
    }

    public CodecManager build() {
        return new CodecManager(new HashMap<>(this.codecs));
    }

    private void appendDefaults() {
        appendByte(Boolean.class, ByteBuf::readBoolean, (o, b) -> o.writeBoolean(b));
        appendByte(Byte.class, ByteBuf::readByte, (o, b) -> o.writeByte(b));
        appendByte(Short.class, ByteBuf::readShort, (o, b) -> o.writeShort(b));
        appendByte(Character.class, ByteBuf::readChar, (o, b) -> o.writeChar(b));
        appendByte(Integer.class, ByteBuf::readInt, ByteBuf::writeInt);
        appendByte(Long.class, ByteBuf::readLong, ByteBuf::writeLong);
        appendByte(Float.class, ByteBuf::readFloat, ByteBuf::writeFloat);
        appendByte(Double.class, ByteBuf::readDouble, ByteBuf::writeDouble);

        append(boolean.class,
               (i, o, a) -> UnsafeAccess.unsafe.putBoolean(o, a, i.readBoolean()),
               (o, i, a) -> o.writeBoolean(UnsafeAccess.unsafe.getBoolean(o, a)));
        append(byte.class,
               (i, o, a) -> UnsafeAccess.unsafe.putByte(o, a, i.readByte()),
               (o, i, a) -> o.writeByte(UnsafeAccess.unsafe.getByte(o, a)));
        append(short.class,
               (i, o, a) -> UnsafeAccess.unsafe.putShort(o, a, i.readShort()),
               (o, i, a) -> o.writeShort(UnsafeAccess.unsafe.getShort(o, a)));
        append(char.class,
               (i, o, a) -> UnsafeAccess.unsafe.putChar(o, a, i.readChar()),
               (o, i, a) -> o.writeChar(UnsafeAccess.unsafe.getChar(o, a)));
        append(int.class,
               (i, o, a) -> UnsafeAccess.unsafe.putInt(o, a, i.readInt()),
               (o, i, a) -> o.writeInt(UnsafeAccess.unsafe.getInt(o, a)));
        append(long.class,
               (i, o, a) -> UnsafeAccess.unsafe.putLong(o, a, i.readLong()),
               (o, i, a) -> o.writeLong(UnsafeAccess.unsafe.getLong(o, a)));
        append(float.class,
               (i, o, a) -> UnsafeAccess.unsafe.putFloat(o, a, i.readFloat()),
               (o, i, a) -> o.writeFloat(UnsafeAccess.unsafe.getFloat(o, a)));
        append(double.class,
               (i, o, a) -> UnsafeAccess.unsafe.putDouble(o, a, i.readDouble()),
               (o, i, a) -> o.writeDouble(UnsafeAccess.unsafe.getDouble(o, a)));

        appendCollection(HashSet.class, HashSet::new);
        appendCollection(LinkedList.class, i -> new LinkedList());
        appendCollection(ArrayList.class, ArrayList::new);

        append(String.class, new StringCodec());
    }
}
