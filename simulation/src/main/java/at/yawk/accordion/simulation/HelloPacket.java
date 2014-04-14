package at.yawk.accordion.simulation;

import at.yawk.accordion.packet.Packet;
import io.netty.buffer.ByteBuf;

/**
 * Test packet.
 *
 * @author Yawkat
 */
public class HelloPacket implements Packet {
    @Override
    public void read(ByteBuf source) {}

    @Override
    public void write(ByteBuf target) {}
}
