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
