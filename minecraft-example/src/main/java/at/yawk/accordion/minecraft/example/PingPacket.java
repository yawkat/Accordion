package at.yawk.accordion.minecraft.example;

/**
 * "Ping"
 *
 * @author Yawkat
 */
public class PingPacket extends AbstractPingPacket {
    public PingPacket(int payload) { super(payload); }

    public PingPacket() {}
}
