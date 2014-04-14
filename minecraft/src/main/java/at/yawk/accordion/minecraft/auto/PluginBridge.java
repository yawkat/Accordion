package at.yawk.accordion.minecraft.auto;

import at.yawk.accordion.minecraft.MinecraftNetManager;
import at.yawk.accordion.minecraft.Server;
import at.yawk.accordion.minecraft.ServerCategory;
import at.yawk.accordion.p2p.Recipient;
import at.yawk.accordion.p2p.RecipientRange;
import at.yawk.accordion.packet.Packet;
import at.yawk.accordion.packet.PacketType;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Implementation of the API.
 *
 * @author Yawkat
 */
public class PluginBridge implements API {
    static PluginBridge instance = null;

    /**
     * RNG used for server ID generation.
     */
    private static final Random RNG = new Random();

    private final PluginConfig config;
    /**
     * All registered packet handlers.
     */
    private final Map<PacketType<?>, Consumer<?>> packetHandlers = new ConcurrentHashMap<>();

    private ApiHelper apiHelper;

    @SuppressWarnings("unchecked")
    public PluginBridge(PluginConfig config) {
        this.config = config;
        Server self = Server.create(config.getType(),
                                    config.getId(RNG.nextLong()),
                                    config.getPublicAddress(),
                                    config.getNetworkPort());

        MinecraftNetManager netManager = new MinecraftNetManager(self, e -> {
            apiHelper.accept(e);
            ((Consumer) packetHandlers.getOrDefault(e.getPacketType(), noop -> {})).accept(e.getPacket());
        });
        apiHelper = new ApiHelper(netManager);
        apiHelper.setup();

        instance = this;
    }

    /**
     * Start the network manager.
     */
    public void start() {
        apiHelper.getNetManager().start();
    }

    @Override
    public <P extends Packet> void registerPacket(String id, Supplier<P> creator, Class<P> type, Consumer<P> listener) {
        PacketType packetType = apiHelper.getNetManager().registerPacket(apiHelper.hash(id), creator, type);
        packetHandlers.put(packetType, listener);
    }

    @Override
    public ServerCategory registerCategory(String name, RecipientRange category) {
        long id = apiHelper.hash(name);
        ServerCategory cat = new ServerCategory() {
            @Override
            public long getId() {
                return id;
            }

            @Override
            public boolean contains(Recipient recipient) {
                return category.contains(recipient);
            }

            @Override
            public boolean containsAll(RecipientRange range) {
                return category.containsAll(range);
            }
        };
        apiHelper.getNetManager().addCategory(cat);
        return cat;
    }

    @Override
    public void transmit(Packet packet, ServerCategory recipients) {
        apiHelper.getNetManager().transmit(recipients, packet);
    }

    /**
     * Use the given plugin channel for peer discovery.
     */
    public void useChannel(Consumer<byte[]> channel) {
        apiHelper.initiateConnection(channel);
    }

    /**
     * Returns a consumer that can be fed all information from peer discovery channels.
     */
    public Consumer<byte[]> getPluginMessageHandler() {
        return apiHelper.getPluginChannelHandler();
    }
}
