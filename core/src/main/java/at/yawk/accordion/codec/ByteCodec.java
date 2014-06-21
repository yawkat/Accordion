package at.yawk.accordion.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Codec for encoding / decoding messages from ByteBufs. Allows encoding to existing ByteBufs to save allocations.
 *
 * @author yawkat
 */
public interface ByteCodec<U> extends Codec<ByteBuf, U> {
    @Override
    default ByteBuf encode(U message) {
        ByteBuf buf = Unpooled.buffer();
        encode(buf, message);
        return buf;
    }

    /**
     * Encode U to an existing ByteBuf.
     */
    void encode(ByteBuf target, U message);
}
