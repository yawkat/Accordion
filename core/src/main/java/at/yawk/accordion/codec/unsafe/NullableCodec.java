package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class NullableCodec<T> implements ByteCodec<T> {
    private final ByteCodec<T> delegate;

    @Override
    public void encode(ByteBuf target, T message) {
        target.writeBoolean(message != null);
        if (message != null) {
            delegate.encode(target, message);
        }
    }

    @Override
    public T decode(ByteBuf encoded) {
        if (!encoded.readBoolean()) {
            return null;
        }
        return delegate.decode(encoded);
    }
}
