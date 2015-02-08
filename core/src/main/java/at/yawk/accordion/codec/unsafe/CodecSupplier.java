/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.unsafe;

import java.util.Optional;

/**
 * @author yawkat
 */
@FunctionalInterface
interface CodecSupplier {
    Optional<UnsafeCodec> getCodec(CodecSupplier registry, FieldWrapper field);

    default Optional<UnsafeCodec> getCodec(FieldWrapper field) {
        return getCodec(this, field);
    }

    default UnsafeCodec getCodecOrThrow(FieldWrapper field) {
        return getCodec(field)
                .orElseThrow(() -> new UnsupportedOperationException("Missing codec for " + field.name()));
    }
}
