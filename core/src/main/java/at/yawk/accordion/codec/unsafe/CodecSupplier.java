package at.yawk.accordion.codec.unsafe;

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

    default UnsafeCodec getCodecOrThrow(FieldWrapper field) {
        return getCodec(field)
                .orElseThrow(() -> new UnsupportedOperationException("Missing codec for " + field.name()));
    }
}
