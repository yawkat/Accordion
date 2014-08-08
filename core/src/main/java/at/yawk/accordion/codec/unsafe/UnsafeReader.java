package at.yawk.accordion.codec.unsafe;

import io.netty.buffer.ByteBuf;

/**
 * @author yawkat
 */
@FunctionalInterface
interface UnsafeReader {
    void read(ByteBuf from, Object to, long offset);
}
