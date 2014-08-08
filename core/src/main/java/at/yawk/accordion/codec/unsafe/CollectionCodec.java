package at.yawk.accordion.codec.unsafe;

import io.netty.buffer.ByteBuf;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.function.IntFunction;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class CollectionCodec<C extends Collection<E>, E> extends UnsafeCodec<C> {
    private final IntFunction<? extends C> factory;
    private final UnsafeCodec<E> componentCodec;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <C extends Collection<E>, E> CodecFactory<C> forType(IntFunction<? extends C> factory) {
        return (manager, field) -> {
            Class<?> para = TypeParameters.getTypeParameter(field, 0);
            return new CollectionCodec<>(factory, (UnsafeCodec) manager.findCodec(para));
        };
    }

    @Override
    public void encode(ByteBuf target, C message) {
        target.writeInt(message.size());
        for (E ele : message) {
            componentCodec.encode(target, ele);
        }
    }

    @Override
    public C decode(ByteBuf encoded) {
        int size = encoded.readInt();
        C collection = factory.apply(size);
        for (int i = 0; i < size; i++) {
            collection.add(componentCodec.decode(encoded));
        }
        return collection;
    }
}
