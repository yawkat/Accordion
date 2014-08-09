package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import java.lang.reflect.Field;
import java.util.Optional;

/**
 * @author yawkat
 */
@FunctionalInterface
interface CodecSupplier {
    Optional<UnsafeCodec> getCodec(CodecSupplier registry, FieldWrapper field);

    default Optional<UnsafeCodec> getCodec(FieldWrapper field) {
        return getCodec(this, field);
    }
}
