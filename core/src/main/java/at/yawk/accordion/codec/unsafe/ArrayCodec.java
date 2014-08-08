package at.yawk.accordion.codec.unsafe;

import io.netty.buffer.ByteBuf;
import java.lang.reflect.Array;

/**
 * @author yawkat
 */
class ArrayCodec<T> extends UnsafeCodec<T> {
    private final Class<T> componentType;
    private final UnsafeCodec componentCodec;
    private final long baseOffset;
    private final long indexScale;

    @SuppressWarnings("unchecked")
    ArrayCodec(Class<T[]> arrayType, UnsafeCodec componentCodec) {
        this.componentType = (Class<T>) arrayType.getComponentType();
        this.componentCodec = componentCodec;

        this.baseOffset = UnsafeAccess.unsafe.arrayBaseOffset(arrayType);
        this.indexScale = UnsafeAccess.unsafe.arrayIndexScale(arrayType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T decode(ByteBuf from) {
        int length = from.readInt();
        T array = (T) Array.newInstance(componentType, length);
        for (int i = 0; i < length; i++) {
            componentCodec.read(from, array, baseOffset + indexScale * i);
        }
        return array;
    }

    @Override
    public void encode(ByteBuf to, T array) {
        int length = Array.getLength(array);
        to.writeInt(length);
        for (int i = 0; i < length; i++) {
            componentCodec.write(to, array, baseOffset + indexScale * i);
        }
    }
}
