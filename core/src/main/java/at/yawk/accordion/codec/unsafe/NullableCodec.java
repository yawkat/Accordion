/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class NullableCodec<T> implements ByteCodec<T> {
    private final ByteCodec<T> delegate;

    @Override
    public void encode(ByteBuf target, T message) {
        target.writeBoolean(message != null);
        if (message != null) {
            delegate.encode(target, message);
        }
    }

    @Override
    public T decode(ByteBuf encoded) {
        if (!encoded.readBoolean()) {
            return null;
        }
        return delegate.decode(encoded);
    }
}
