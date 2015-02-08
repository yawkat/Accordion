/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.compression;

import io.netty.buffer.ByteBuf;
import lombok.Getter;

/**
 * No-operation Compressor implementation, performs no compression.
 *
 * @author yawkat
 */
public class VoidCompressor implements Compressor {
    @Getter private static final Compressor instance = new VoidCompressor();

    private VoidCompressor() {}

    @Override
    public ByteBuf encode(ByteBuf message) {
        return message;
    }

    @Override
    public ByteBuf decode(ByteBuf encoded) {
        return encoded;
    }
}
