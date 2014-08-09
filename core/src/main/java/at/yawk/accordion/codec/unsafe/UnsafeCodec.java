package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;

/**
 * @author yawkat
 */
interface UnsafeCodec {
    void read(ByteBuf from, Object to, long offset);

    void write(ByteBuf to, Object from, long offset);

    ByteCodec<?> toByteCodec();
}
