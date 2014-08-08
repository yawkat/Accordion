package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author yawkat
 */
class ReflectiveByteCodec<T> implements ByteCodec<T> {
    private final Class<T> of;
    private final ClassSchemaEntry[] entries;

    @SuppressWarnings("rawtypes")
    public ReflectiveByteCodec(Class<T> of, CodecManager serializers) {
        this.of = of;

        this.entries = declaredFields(of)
                .map(field -> {
                    long offset = UnsafeAccess.unsafe.objectFieldOffset(field);
                    UnsafeCodec codec = serializers.findCodec(field);
                    return new ClassSchemaEntry(offset, codec);
                }).toArray(ClassSchemaEntry[]::new);
    }

    private Stream<Field> declaredFields(Class<?> clazz) {
        Stream<Field> own = Arrays.stream(clazz.getDeclaredFields());
        if (clazz != Object.class) {
            own = Stream.concat(declaredFields(clazz.getSuperclass()), own);
        }
        return own.filter(field -> !Modifier.isStatic(field.getModifiers()));
    }

    @Override
    public void encode(ByteBuf target, T message) {
        for (ClassSchemaEntry entry : entries) {
            entry.codec.write(target, message, entry.offset);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T decode(ByteBuf encoded) {
        try {
            T o = (T) UnsafeAccess.unsafe.allocateInstance(of);
            for (ClassSchemaEntry entry : entries) {
                entry.codec.read(encoded, o, entry.offset);
            }
            return o;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
