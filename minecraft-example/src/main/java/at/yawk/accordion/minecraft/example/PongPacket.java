package at.yawk.accordion.minecraft.example;

/**
 * "Pong"
 *
 * @author Yawkat
 */
public class PongPacket extends AbstractPingPacket {
    public PongPacket(int payload) { super(payload); }

    public PongPacket() {}
}
