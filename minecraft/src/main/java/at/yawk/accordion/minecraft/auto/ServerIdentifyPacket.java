package at.yawk.accordion.minecraft.auto;

import at.yawk.accordion.minecraft.MinecraftNetManager;
import at.yawk.accordion.minecraft.Server;
import at.yawk.accordion.minecraft.ServerCategory;
import at.yawk.accordion.packet.Packet;
import at.yawk.accordion.packet.StreamUtil;
import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Packet used to add a server to the distributed server database. Sent whenever a new server joins via peer discovery.
 *
 * @author Yawkat
 */
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
class ServerIdentifyPacket implements Packet {
    private final MinecraftNetManager netManager;
    private Server server;
    private boolean created = false;

    @Override
    public void read(ByteBuf source) {
        byte categoryId = source.readByte();
        ServerCategory category = netManager.getCategory(categoryId);
        long id = StreamUtil.readVarIntLong(source);
        InetAddress address;
        try {
            address = InetAddress.getByAddress(StreamUtil.readDynamicByteArray(source));
        } catch (UnknownHostException e) {
            throw new Error(e);
        }
        int port = source.readUnsignedShort();

        server = netManager.getServer(category, id);
        if (server == null) {
            server = Server.create(category, id, address, port);
            created = true;
        }
    }

    @Override
    public void write(ByteBuf target) {
        StreamUtil.writeVarInt(target, server.getCategory().getId());
        StreamUtil.writeVarInt(target, server.getId());
        StreamUtil.writeDynamicByteArray(target, server.getHost().getAddress());
        target.writeShort(server.getNetworkPort());
    }
}
