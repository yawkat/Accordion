/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.packet;

import io.netty.buffer.ByteBuf;

/**
 * Base packet interface. All packets should implement this (assuming you use #MessengerPacketChannel).
 *
 * @author yawkat
 */
public interface Packet {
    /**
     * Read this packet from the given input bytes.
     */
    void read(ByteBuf buf);

    /**
     * Write this packet to the given stream.
     */
    void write(ByteBuf buf);
}
