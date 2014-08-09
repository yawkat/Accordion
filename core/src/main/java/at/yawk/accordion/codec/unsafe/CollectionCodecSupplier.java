package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.function.IntFunction;

/**
 * @author yawkat
 */
class CollectionCodecSupplier<C extends Collection<?>> extends GenericCodecSupplier<C> {
    private final IntFunction<? extends C> factory;

    public CollectionCodecSupplier(Class<C> type, IntFunction<? extends C> factory) {
        super(type);
        this.factory = factory;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected ByteCodec<C> createCodec(CodecSupplier registry, FieldWrapper field) {
        FieldWrapper componentType = field.genericTypeOrThrow(0);
        ByteCodec componentCodec = registry.getCodecOrThrow(componentType).toByteCodec();

        return new ByteCodec<C>() {
            @Override
            public void encode(ByteBuf target, C message) {
                target.writeInt(message.size());
                for (Object o : message) {
                    componentCodec.encode(target, o);
                }
            }

            @Override
            public C decode(ByteBuf encoded) {
                int size = encoded.readInt();
                Collection collection = factory.apply(size);
                for (int i = 0; i < size; i++) {
                    collection.add(componentCodec.decode(encoded));
                }
                return (C) collection;
            }
        };
    }
}
