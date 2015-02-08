/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.unsafe;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class CachedCodecSupplier implements CodecSupplier {
    private final CodecSupplier delegate;
    private final Map<FieldWrapper, Optional<UnsafeCodec>> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<UnsafeCodec> getCodec(CodecSupplier registry, FieldWrapper field) {
        return cache.computeIfAbsent(field, f -> delegate.getCodec(this, f));
    }
}
