package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class UnsafeByteCodec<T> extends UnsafeCodec<T> {
    private final ByteCodec<T> codec;

    @Override
    public void read(ByteBuf from, Object to, long offset) {
        T val = codec.decode(from);
        UnsafeAccess.unsafe.putObject(to, offset, val);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(ByteBuf to, Object from, long offset) {
        T val = (T) UnsafeAccess.unsafe.getObject(from, offset);
        codec.encode(to, val);
    }

    @Override
    public void encode(ByteBuf target, T message) {
        codec.encode(target, message);
    }

    @Override
    public T decode(ByteBuf encoded) {
        return codec.decode(encoded);
    }
}
