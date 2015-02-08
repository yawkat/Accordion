/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;

/**
 * @author yawkat
 */
interface UnsafeCodec {
    void read(ByteBuf from, Object to, long offset);

    void write(ByteBuf to, Object from, long offset);

    ByteCodec<?> toByteCodec();
}
