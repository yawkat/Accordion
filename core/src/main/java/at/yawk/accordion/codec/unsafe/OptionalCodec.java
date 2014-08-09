package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
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
    @SuppressWarnings("unchecked")
    protected ByteCodec<Optional> createCodec(CodecSupplier registry, FieldWrapper field) {
        FieldWrapper componentType = field.genericType(0).get();
        ByteCodec componentCodec = registry.getCodecOrThrow(componentType).toByteCodec();
        return new ByteCodec<Optional>() {
            @Override
            public void encode(ByteBuf target, Optional message) {
                boolean present = message.isPresent();
                target.writeBoolean(present);
                if (present) {
                    componentCodec.encode(target, message.get());
                }
            }

            @Override
            public Optional decode(ByteBuf encoded) {
                boolean present = encoded.readBoolean();
                if (present) {
                    Object v = componentCodec.decode(encoded);
                    return Optional.of(v);
                } else {
                    return Optional.empty();
                }
            }
        };
    }
}
