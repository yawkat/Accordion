/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.compression;

import at.yawk.accordion.codec.Codec;
import io.netty.buffer.ByteBuf;

/**
 * Byte-Byte codec that compresses it's encoded form. Input ByteBufs should not be reused as they may either be drained
 * while (de)compression takes place, returned again (in the case of VoidCompressor) or even not touched at all.
 *
 * @author yawkat
 */
public interface Compressor extends Codec<ByteBuf, ByteBuf> {}
