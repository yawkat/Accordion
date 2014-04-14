package at.yawk.accordion.minecraft;

import at.yawk.accordion.p2p.ConnectionPolicy;
import at.yawk.accordion.p2p.Envelope;
import at.yawk.accordion.p2p.P2PManager;
import at.yawk.accordion.packet.Packet;
import at.yawk.accordion.packet.PacketType;
import com.google.common.collect.Maps;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.mahout.math.map.AbstractLongObjectMap;
import org.apache.mahout.math.map.OpenLongObjectHashMap;

/**
 * Main network manager for a minecraft server network.
 *
 * @author Yawkat
 */
public class MinecraftNetManager {
    /**
     * The underlying network manager.
     */
    private final P2PManager p2pManager;

    /**
     * All categories of servers (used for deserialization).
     */
    private final AbstractLongObjectMap<ServerCategory> categories = new OpenLongObjectHashMap<>();
    /**
     * All servers by main category and ID.
     */
    private final Map<ServerCategory, AbstractLongObjectMap<Server>> servers = Maps.newHashMap();

    static {
        // prepare logging
        Logger.getRootLogger().addAppender(new ConsoleAppender(new SimpleLayout()));
        Logger.getRootLogger().setLevel(Level.DEBUG);
    }

    public MinecraftNetManager(Server self, ConnectionPolicy connectionPolicy, Consumer<Envelope> packetHandler) {
        p2pManager = new P2PManager(self, connectionPolicy, new McRecipientFactory(this));
        p2pManager.setMessageHandler(packetHandler);

        addCategory(ServerCategory.Default.ALL);
        addCategory(ServerCategory.Default.BUKKIT);
        addCategory(ServerCategory.Default.BUNGEE);
    }

    /**
     * Constructor.
     *
     * @param self          This server.
     * @param packetHandler The packet handler (called every time a packet is received).
     */
    public MinecraftNetManager(Server self, Consumer<Envelope> packetHandler) {
        this(self, new BackboneConnectionPolicy(), packetHandler);
    }

    /**
     * Register a category so it can be deserialized. Missing categories will cause errors.
     */
    public synchronized void addCategory(@NonNull ServerCategory category) {
        categories.put(category.getId(), category);
    }

    /**
     * Register a server so we can connect the network to it. Also registers its main category, if necessary.
     */
    public synchronized void addPeer(@NonNull Server server) {
        addCategory(server.getCategory());
        if (!servers.containsKey(server.getCategory())) {
            servers.put(server.getCategory(), new OpenLongObjectHashMap<>());
        }
        servers.get(server.getCategory()).put(server.getId(), server);
        p2pManager.registerPeer(server);
    }

    /**
     * Add all servers in the given stream, consuming it.
     */
    public void addPeers(Stream<Server> servers) {
        servers.forEach(this::addPeer);
    }

    /**
     * Add all servers in the given Collection.
     */
    public void addPeers(Collection<Server> servers) {
        addPeers(servers.stream());
    }

    /**
     * Add all given servers.
     */
    public void addPeers(Server... servers) {
        addPeers(Stream.of(servers));
    }

    /**
     * Register a new packet type.
     */
    public <P extends Packet> PacketType registerPacket(long id, Supplier<P> creator, Class<P> type) {
        PacketType ptype = PacketType.create(id, type, creator);
        p2pManager.getPacketManager().registerPacketType(ptype);
        return ptype;
    }

    /**
     * Register a new packet type. The given type must have an empty constructor.
     */
    public <P extends Packet> void registerPacket(long id, Class<P> type) {
        try {
            Constructor<P> constructor = type.getConstructor();
            registerPacket(id, () -> {
                try {
                    return constructor.newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new Error(e);
                }
            }, type);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Send an envelope away.
     */
    public void transmitEnvelope(Envelope envelope) {
        p2pManager.transmit(envelope);
    }

    /**
     * Send a packet to all recipients in the given category.
     */
    public void transmit(ServerCategory recipients, Packet packet) {
        transmitEnvelope(Envelope.create(p2pManager.getPacketManager(), recipients, packet));
    }

    /**
     * Broadcast a packet to all servers on the network.
     */
    public void broadcast(Packet packet) {
        transmit(ServerCategory.Default.ALL, packet);
    }

    /**
     * Start the network. All peers should already be registered.
     */
    public void start() {
        p2pManager.start();
    }

    /**
     * Return a stream of all known categories.
     */
    public Stream<ServerCategory> listCategories() {
        return categories.values().stream();
    }

    /**
     * Find the category with the given ID.
     *
     * @return the category or null if it is not known.
     */
    @Nullable
    public ServerCategory getCategory(long id) {
        return categories.get(id);
    }

    /**
     * Find the server with the given primary category and ID.
     *
     * @return The server or null if it is unknown.
     */
    @Nullable
    public Server getServer(ServerCategory category, long id) {
        AbstractLongObjectMap<Server> inCat = servers.get(category);
        return inCat == null ? null : inCat.get(id);
    }

    /**
     * This server.
     */
    public Server getSelf() {
        return (Server) p2pManager.getSelf();
    }

    /**
     * Returns a stream of all known servers.
     */
    public Stream<Server> getServers() {
        return servers.values().stream().map(AbstractLongObjectMap::values).flatMap(l -> l.stream());
    }
}
