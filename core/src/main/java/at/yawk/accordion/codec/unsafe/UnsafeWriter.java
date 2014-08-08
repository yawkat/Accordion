package at.yawk.accordion.codec.unsafe;

import io.netty.buffer.ByteBuf;

/**
 * @author yawkat
 */
@FunctionalInterface
interface UnsafeWriter {
    void write(ByteBuf to, Object from, long offset);
}
