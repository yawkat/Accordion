package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class CommonObjectCodec<T> implements ByteCodec<T> {
    private final Class<T> type;
    private final OffsetField[] fields;

    /**
     * Follows the contract of CodecSupplier to be used as a method reference.
     *
     * Returns a generic codec if the given field is serializable, an empty optional otherwise (abstract class,
     * interface etc). The returned codec will only serialize objects of the exact type of the field: it does not
     * send class name to preserve bandwidth so it has to throw in cases where subclasses are given to #encode.
     */
    @SuppressWarnings("unchecked")
    public static Optional<UnsafeCodec> factory(CodecSupplier registry, FieldWrapper field) {
        Class<?> type = field.type();
        if (Modifier.isAbstract(type.getModifiers()) ||
            type.isInterface() ||
            type.isArray() ||
            type.isAnonymousClass() ||
            type.isLocalClass() ||
            type.isEnum()) {
            return Optional.empty();
        }
        return Optional.of(new UnsafeByteCodec(create(registry, type)));
    }

    public static <T> ByteCodec<T> create(CodecSupplier registry, Class<T> clazz) {
        OffsetField[] fields = declaredFields(clazz)
                .map(field -> {
                    FieldWrapper wrapper = FieldWrapper.field(field);
                    long offset = UnsafeAccess.unsafe.objectFieldOffset(field);
                    UnsafeCodec codec = registry.getCodecOrThrow(wrapper);
                    return new OffsetField(offset, codec);
                })
                .toArray(OffsetField[]::new);
        return new CommonObjectCodec<>(clazz, fields);
    }

    private static Stream<Field> declaredFields(Class<?> clazz) {
        Stream<Field> own = Arrays.stream(clazz.getDeclaredFields());
        if (clazz != Object.class) {
            own = Stream.concat(declaredFields(clazz.getSuperclass()), own);
        }
        return own.filter(field -> !Modifier.isStatic(field.getModifiers()));
    }

    @Override
    public void encode(ByteBuf target, T message) {
        if (message.getClass() != type) {
            throw new UnsupportedOperationException(
                    "GenericObjectCodec cannot serialize subclass " + message.getClass() + " of " + type);
        }

        for (OffsetField field : this.fields) {
            field.codec.write(target, message, field.offset);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T decode(ByteBuf encoded) {
        T instance;
        try {
            instance = (T) UnsafeAccess.unsafe.allocateInstance(type);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }

        for (OffsetField field : this.fields) {
            field.codec.read(encoded, instance, field.offset);
        }

        return instance;
    }

    @RequiredArgsConstructor
    private static class OffsetField {
        private final long offset;
        private final UnsafeCodec codec;
    }
}
