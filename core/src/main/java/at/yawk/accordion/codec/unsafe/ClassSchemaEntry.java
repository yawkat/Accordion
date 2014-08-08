package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import java.lang.reflect.Field;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class ClassSchemaEntry {
    final long offset;
    final UnsafeCodec codec;
}
