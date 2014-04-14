package at.yawk.accordion.minecraft.auto;

import at.yawk.accordion.minecraft.MinecraftNetManager;
import at.yawk.accordion.minecraft.Server;
import at.yawk.accordion.p2p.Envelope;
import at.yawk.accordion.packet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.Getter;

/**
 * Peer discovery helper for the minecraft plugins, using plugin channels.
 *
 * @author Yawkat
 */
@Getter
class ApiHelper implements Consumer<Envelope> {
    private final MinecraftNetManager netManager;
    private final Consumer<byte[]> pluginChannelHandler;

    public ApiHelper(MinecraftNetManager netManager) {
        this.netManager = netManager;
        pluginChannelHandler = a -> {
            ServerIdentifyPacket packet = new ServerIdentifyPacket(this.netManager);
            packet.read(Unpooled.copiedBuffer(a));
            acceptPacket(packet);
            this.netManager.broadcast(packet);
        };
    }

    /**
     * Register the identify packet to enable automated peer discovery.
     */
    public void setup() {
        netManager.registerPacket(hash("accordion.identifyServer"),
                                  () -> new ServerIdentifyPacket(netManager),
                                  ServerIdentifyPacket.class);
    }

    /**
     * Hash a string to create a unique id from it.
     */
    public long hash(String id) {
        long code = 0;
        for (int i = 0; i < id.length(); i++) {
            code = code * 31 + id.charAt(i);
        }
        return code;
    }

    /**
     * Should be called when a message is received via the network.
     */
    @Override
    public void accept(Envelope envelope) {
        Packet packet = envelope.getPacket();
        acceptPacket(packet);
    }

    private void acceptPacket(Packet packet) {
        if (packet instanceof ServerIdentifyPacket) {
            if (((ServerIdentifyPacket) packet).isCreated()) {
                netManager.addPeer(((ServerIdentifyPacket) packet).getServer());
            }
        }
    }

    /**
     * Send some peer discovery info to the given plugin channel.
     */
    public void initiateConnection(Consumer<byte[]> pluginChannel) {
        Stream<Server> servers = Stream.concat(Stream.of(netManager.getSelf()), netManager.getServers()).distinct();

        servers.forEach(s -> {
            ServerIdentifyPacket packet = new ServerIdentifyPacket(netManager, s, false);
            ByteBuf data = Unpooled.buffer();
            packet.write(data);
            pluginChannel.accept(data.array());
        });
    }
}
