package at.yawk.accordion.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.apache.mahout.math.map.AbstractIntObjectMap;
import org.apache.mahout.math.map.OpenIntObjectHashMap;

/**
 * Packet registry used for identification of different types of packets.
 *
 * @author Yawkat
 */
public class PacketManager {
    /**
     * All packet types mapped to their unique ID.
     */
    private final AbstractIntObjectMap<PacketType> packetTypesById = new OpenIntObjectHashMap<>();
    /**
     * All packet types mapped to their Packet subclass.
     */
    private final Map<Class<?>, PacketType> packetTypesByClass = new HashMap<>();

    /**
     * Register a new PacketType.
     */
    public void registerPacketType(PacketType type) {
        assert !packetTypesById.containsKey(type.getId()) : type;

        packetTypesById.put(type.getId(), type);
        packetTypesByClass.put(type.getPacketClass(), type);
    }

    /**
     * Serialize a packet, including packet id, so it can be read by #readPacket.
     */
    public ByteBuf writePacket(Packet packet) {
        PacketType type = findPacketType(packet);
        assert type != null : packet.getClass();

        ByteBuf target = Unpooled.buffer();
        StreamUtil.writeVarInt(target, type.getId());
        packet.write(target);
        return target;
    }

    /**
     * Deserialize a packet written by #writePacket.
     */
    public Packet readPacket(ByteBuf input) {
        int id = StreamUtil.readVarInt(input);
        PacketType type = packetTypesById.get(id);
        assert type != null : id;
        Packet packet = type.get();
        packet.read(input);
        return packet;
    }

    /**
     * Find the associated type of the given packet.
     *
     * @return The packet type or null if the packet is invalid.
     */
    @Nullable
    public PacketType findPacketType(@NonNull Packet packet) {
        return findPacketType(packet.getClass());
    }

    /**
     * Recursively find the PacketType for the given class.
     */
    @Nullable
    private PacketType findPacketType(@NonNull Class<?> type) {
        @SuppressWarnings("SuspiciousMethodCalls") PacketType found = packetTypesByClass.get(type);
        if (found != null) {
            return found;
        } else if (type == Object.class) {
            return null;
        } else {
            return findPacketType(type.getSuperclass());
        }
    }
}
