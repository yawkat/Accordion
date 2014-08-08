package at.yawk.accordion.codec.unsafe;

import java.lang.reflect.Field;

/**
 * @author yawkat
 */
interface CodecFactory<T> {
    UnsafeCodec<T> create(CodecManager manager, Field field);
}
