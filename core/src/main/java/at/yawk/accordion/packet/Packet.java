package at.yawk.accordion.packet;

import io.netty.buffer.ByteBuf;

/**
 * A serializable Packet.
 *
 * @author Yawkat
 */
public interface Packet {
    /**
     * Read the information of this packet from the given byte blob.
     */
    void read(ByteBuf source);

    /**
     * Write the data in this packet to the given stream.
     */
    void write(ByteBuf target);
}
