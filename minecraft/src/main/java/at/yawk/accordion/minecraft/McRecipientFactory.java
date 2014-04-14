package at.yawk.accordion.minecraft;

import at.yawk.accordion.p2p.Recipient;
import at.yawk.accordion.p2p.RecipientFactory;
import at.yawk.accordion.p2p.RecipientRange;
import at.yawk.accordion.packet.StreamUtil;
import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.RequiredArgsConstructor;

/**
 * RecipientFactory that creates Servers and ServerCategories.
 *
 * @author Yawkat
 */
@RequiredArgsConstructor
class McRecipientFactory implements RecipientFactory {
    private final MinecraftNetManager netManager;

    @Override
    public ServerCategory readRange(ByteBuf source) {
        long id = StreamUtil.readVarIntLong(source);
        return netManager.getCategory(id);
    }

    @Override
    public Server read(ByteBuf source, boolean useCache) {
        ServerCategory category = readRange(source);
        long id = StreamUtil.readVarIntLong(source);
        if (useCache) { return netManager.getServer(category, id); }

        InetAddress address;
        try {
            address = InetAddress.getByAddress(StreamUtil.readDynamicByteArray(source));
        } catch (UnknownHostException e) {
            throw new Error(e);
        }
        int port = source.readUnsignedShort();
        return Server.create(category, id, address, port);
    }

    @Override
    public void writeRange(ByteBuf target, RecipientRange recipients) {
        StreamUtil.writeVarInt(target, ((ServerCategory) recipients).getId());
    }

    @Override
    public void write(ByteBuf target, Recipient recipient, boolean useCache) {
        Server server = (Server) recipient;

        writeRange(target, server.getCategory());
        StreamUtil.writeVarInt(target, server.getId());
        if (useCache) { return; }

        StreamUtil.writeDynamicByteArray(target, server.getHost().getAddress());
        target.writeShort(server.getNetworkPort());
    }
}
