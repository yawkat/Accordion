package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import java.util.Optional;

/**
 * @author yawkat
 */
@SuppressWarnings("rawtypes")
class OptionalCodec extends GenericCodecSupplier<Optional> {
    public OptionalCodec() {
        super(Optional.class);
    }

    @Override
    protected ByteCodec<Optional> createCodec(CodecSupplier registry, FieldWrapper field) {
        FieldWrapper componentType = field.genericType(0).get();
        return null;
    }
}
