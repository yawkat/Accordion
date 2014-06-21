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
