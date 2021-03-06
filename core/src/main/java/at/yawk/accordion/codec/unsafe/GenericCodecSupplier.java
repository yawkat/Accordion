/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import java.lang.reflect.Field;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
abstract class GenericCodecSupplier<T> implements CodecSupplier {
    private final Class<T> type;

    @Override
    public Optional<UnsafeCodec> getCodec(CodecSupplier registry, FieldWrapper field) {
        if (field.type() == type) {
            return Optional.of(new UnsafeByteCodec<>(createCodec(registry, field)));
        }
        return Optional.empty();
    }

    protected abstract ByteCodec<T> createCodec(CodecSupplier registry, FieldWrapper field);
}
