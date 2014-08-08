package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class CodecManager {
    @Getter private static final CodecManager defaultManager = CodecManagerBuilder.create().build();

    private final Map<Class<?>, CodecFactory<?>> codecs;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    UnsafeCodec<?> findCodec(Field field) {
        UnsafeCodec<?> result = findCodec(field.getType(), field);
        if (result == null) {
            throw new NoSuchElementException("No codec for " + field.getType().getName());
        }
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    UnsafeCodec<?> findCodec(Class<?> clazz) {
        UnsafeCodec<?> result = findCodec(clazz, null);
        if (result == null) {
            throw new NoSuchElementException("No codec for " + clazz.getName());
        }
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private UnsafeCodec<?> findCodec(Class<?> clazz, Field field) {
        if (clazz.isArray()) {
            return new ArrayCodec((Class) clazz, findCodec(clazz.getComponentType()));
        }

        if (!codecs.containsKey(clazz)) {
            return null;
        }
        return codecs.get(clazz).create(this, field);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    <T> ByteCodec<T> findFirstLevelCodec(Class<T> of) {
        UnsafeCodec<?> codec = findCodec(of, null);
        if (codec == null) {
            return new ReflectiveByteCodec<>(of, this);
        }
        return (ByteCodec) codec;
    }
}
