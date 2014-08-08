package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;

/**
 * @author yawkat
 */
abstract class UnsafeCodec<T> implements UnsafeReader, UnsafeWriter, ByteCodec<T> {
    /**
     * Only for primitives!
     */
    public static UnsafeCodec<Object> build(UnsafeReader reader, UnsafeWriter writer) {
        return new UnsafeCodec<Object>() {
            @Override
            public void read(ByteBuf from, Object to, long offset) {
                reader.read(from, to, offset);
            }

            @Override
            public void write(ByteBuf to, Object from, long offset) {
                writer.write(to, from, offset);
            }

            @Override
            public void encode(ByteBuf target, Object message) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object decode(ByteBuf encoded) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public void read(ByteBuf from, Object to, long offset) {
        T val = decode(from);
        UnsafeAccess.unsafe.putObject(to, offset, val);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(ByteBuf to, Object from, long offset) {
        T val = (T) UnsafeAccess.unsafe.getObject(from, offset);
        encode(to, val);
    }
}
