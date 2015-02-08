/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Array;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class ArrayCodec implements ByteCodec<Object> {
    private final Class<?> componentType;
    private final UnsafeCodec componentCodec;
    private final long baseOffset;
    private final long indexScale;

    public static Optional<UnsafeCodec> factory(CodecSupplier registry, FieldWrapper field) {
        Class<?> arrayType = field.type();
        if (arrayType.isArray()) {
            Class<?> componentType = arrayType.getComponentType();
            FieldWrapper componentWrapper = FieldWrapper.clazz(componentType);
            UnsafeCodec componentCodec = registry.getCodecOrThrow(componentWrapper);
            long baseOffset = UnsafeAccess.unsafe.arrayBaseOffset(arrayType);
            long indexScale = UnsafeAccess.unsafe.arrayIndexScale(arrayType);
            ArrayCodec codec = new ArrayCodec(componentType, componentCodec, baseOffset, indexScale);
            return Optional.of(new UnsafeByteCodec<>(codec));
        }
        return Optional.empty();
    }

    @Override
    public void encode(ByteBuf target, Object message) {
        int length = Array.getLength(message);
        target.writeInt(length);
        long offset = baseOffset;
        for (int i = 0; i < length; i++) {
            componentCodec.write(target, message, offset);
            offset += indexScale;
        }
    }

    @Override
    public Object decode(ByteBuf encoded) {
        int length = encoded.readInt();
        Object array = Array.newInstance(componentType, length);
        long offset = baseOffset;
        for (int i = 0; i < length; i++) {
            componentCodec.read(encoded, array, offset);
            offset += indexScale;
        }
        return array;
    }
}
