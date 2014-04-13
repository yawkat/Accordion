package at.yawk.accordion.packet;

import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * A packet type is used to uniquely identify each kind of packet an application uses and to create new instances of
 * it.
 *
 * @author Yawkat
 */
@RequiredArgsConstructor(staticName = "create")
@EqualsAndHashCode(of = "id")
@ToString(of = "id")
public class PacketType<PacketT extends Packet> implements Supplier<PacketT> {
    /**
     * The unique ID of this type.
     */
    @Getter private final int id;
    /**
     * The class that each packet of this type extends.
     */
    @Getter private final Class<PacketT> packetClass;
    /**
     * A supplier that returns a new Packet of this type that is ready to be read.
     */
    private final Supplier<PacketT> packetCreator;

    /**
     * Create a new Packet that is ready to be read.
     */
    @Override
    public PacketT get() {
        return packetCreator.get();
    }
}
