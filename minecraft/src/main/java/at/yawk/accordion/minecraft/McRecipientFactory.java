package at.yawk.accordion.minecraft;

import at.yawk.accordion.p2p.Recipient;
import at.yawk.accordion.p2p.RecipientFactory;
import at.yawk.accordion.p2p.RecipientRange;
import io.netty.buffer.ByteBuf;
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
        byte id = source.readByte();
        return netManager.getCategories().get(id);
    }

    @Override
    public Server read(ByteBuf source) {
        ServerCategory category = readRange(source);
        short id = source.readShort();
        return netManager.getServers().get(category).get(id);
    }

    @Override
    public void writeRange(ByteBuf target, RecipientRange recipients) {
        target.writeByte(((ServerCategory) recipients).getId());
    }

    @Override
    public void write(ByteBuf target, Recipient recipient) {
        writeRange(target, ((Server) recipient).getCategory());
        target.writeShort(((Server) recipient).getId());
    }
}
