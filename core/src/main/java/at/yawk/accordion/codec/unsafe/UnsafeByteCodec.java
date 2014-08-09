package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * @author yawkat
 */
@ToString
@RequiredArgsConstructor
class UnsafeByteCodec<T> implements UnsafeCodec {
    private final ByteCodec<T> delegate;

    @Override
    public void read(ByteBuf from, Object to, long offset) {
        T value = delegate.decode(from);
        UnsafeAccess.unsafe.putObject(to, offset, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(ByteBuf to, Object from, long offset) {
        T value = (T) UnsafeAccess.unsafe.getObject(from, offset);
        delegate.encode(to, value);
    }

    @Override
    public ByteCodec<?> toByteCodec() {
        return delegate;
    }
}
