package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * @author yawkat
 */
class MapCodec<M extends Map<?, ?>> extends GenericCodecSupplier<M> {
    private final IntFunction<? extends M> factory;

    public MapCodec(Class<M> type, IntFunction<? extends M> factory) {
        super(type);
        this.factory = factory;
    }

    @Override
    protected ByteCodec<M> createCodec(CodecSupplier registry, FieldWrapper field) {
        FieldWrapper keyType = field.genericType(0)
                .orElseThrow(() -> new UnsupportedOperationException(
                        "Could not resolve generic type of " + field.name()));
        ByteCodec keyCodec = registry.getCodec(keyType)
                .orElseThrow(() -> new UnsupportedOperationException("Cannot serialize " + keyType.name()))
                .toByteCodec();
        FieldWrapper valueType = field.genericType(0)
                .orElseThrow(() -> new UnsupportedOperationException(
                        "Could not resolve generic type of " + field.name()));
        ByteCodec valueCodec = registry.getCodec(valueType)
                .orElseThrow(() -> new UnsupportedOperationException("Cannot serialize " + valueType.name()))
                .toByteCodec();

        return new ByteCodec<M>() {
            @Override
            public void encode(ByteBuf target, M message) {
                target.writeInt(message.size());
                for (Map.Entry o : message.entrySet()) {
                    keyCodec.encode(target, o.getKey());
                    valueCodec.encode(target, o.getValue());
                }
            }

            @Override
            public M decode(ByteBuf encoded) {
                int size = encoded.readInt();
                Map map = factory.apply(size);
                for (int i = 0; i < size; i++) {
                    Object k = keyCodec.decode(encoded);
                    Object v = valueCodec.decode(encoded);
                    map.put(k, v);
                }
                return (M) map;
            }
        };
    }
}
