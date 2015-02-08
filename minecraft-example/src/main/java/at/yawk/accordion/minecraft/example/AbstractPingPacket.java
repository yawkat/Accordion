/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.minecraft.example;

import at.yawk.accordion.codec.packet.Packet;
import io.netty.buffer.ByteBuf;

/**
 * A packet that contains a number. Provides an example for packet serialization.
 *
 * @author Yawkat
 */
public abstract class AbstractPingPacket implements Packet {
    private int payload;

    public AbstractPingPacket(int payload) { this.payload = payload; }

    /**
     * An empty constructor is required.
     */
    public AbstractPingPacket() {}

    @Override
    public void read(ByteBuf source) {
        payload = source.readInt();
    }

    @Override
    public void write(ByteBuf target) {
        target.writeInt(payload);
    }

    public int getPayload() {
        return payload;
    }
}
