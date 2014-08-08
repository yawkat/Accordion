package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@Getter
@RequiredArgsConstructor
class ClassData<T> {
    private final Collection<Consumer<T>> listeners = Collections.synchronizedCollection(new ArrayList<>());
    private final ByteCodec<T> codec;
}
