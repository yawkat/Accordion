/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class EnumCodec<E extends Enum<E>> implements ByteCodec<E> {
    private final Class<E> type;

    @SuppressWarnings("unchecked")
    public static Optional<UnsafeCodec> factory(CodecSupplier registry, FieldWrapper field) {
        if (field.type().isEnum()) {
            return Optional.of(new UnsafeByteCodec(new EnumCodec(field.type())));
        }
        return Optional.empty();
    }

    @Override
    public void encode(ByteBuf target, E message) {
        target.writeShort(message.ordinal());
    }

    @Override
    public E decode(ByteBuf encoded) {
        return type.getEnumConstants()[encoded.readUnsignedShort()];
    }
}
