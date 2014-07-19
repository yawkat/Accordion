package at.yawk.accordion.minecraft;

import at.yawk.accordion.Log;
import at.yawk.accordion.codec.packet.MessengerPacketChannel;
import at.yawk.accordion.codec.packet.PacketChannel;
import at.yawk.accordion.compression.Compressor;
import at.yawk.accordion.distributed.ConnectionListenerFactory;
import at.yawk.accordion.distributed.LocalNode;
import at.yawk.accordion.distributed.LocalNodeBuilder;
import at.yawk.accordion.distributed.Node;
import com.google.common.base.Preconditions;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import lombok.AccessLevel;
import lombok.Delegate;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minecraft-specific API for Accordion. Can be instantiated using #create or the AccordionBukkit and AccordionBungee
 * classes.
 *
 * @author yawkat
 */
public class AccordionApi implements PacketChannel {
    /**
     * Offset from the minecraft listen port that is used for our listener by default.
     */
    private static final int DEFAULT_MINECRAFT_PORT_OFFSET = 653; // arbitrary prime to avoid port collisions

    /**
     * Plugin channel used for peer discovery.
     */
    static final String PEER_DISCOVERY_PLUGIN_CHANNEL = "at.yawk.accordion.pd";

    /**
     * Tier used to identify bukkit nodes. By default, these nodes will connect to the bungee nodes as clients.
     */
    public static final int DEFAULT_TIER_BUKKIT = 0;
    /**
     * Tier used to identify bungee nodes. By default, these nodes will provide access to the network for bukkit nodes
     * and connect to each other.
     */
    public static final int DEFAULT_TIER_BUNGEE = 1;

    /**
     * Address this server is available under from all other nodes.
     */
    private InetAddress externalAddress;
    /**
     * Interface this server should listen on if it is a "hub" node, something like "127.0.0.1" or "0.0.0.0".
     */
    private InetAddress listenAddress;
    /**
     * The tier / group of this server.
     */
    private int tier;
    /**
     * The port our listener should operate on if we are a "hub" node.
     */
    private int port;
    /**
     * Whether to autostart this server after all plugins are loaded.
     */
    private boolean autoStart = true;
    /**
     * Whether we should listen for remote connections.
     */
    private boolean listen;
    /**
     * Our logger.
     */
    private Logger logger = Log.getDefaultLogger();
    /**
     * ConnectionListener specification used to build the network.
     */
    private ConnectionListenerFactory connectionListenerFactory = DefaultMinecraftConnectionListener::create;

    /**
     * The local node.
     */
    @Getter private LocalNode localNode;
    /**
     * The PacketChannel used to send and receive packets through our LocalNode.
     */
    @Delegate @Getter private PacketChannel channel;

    /**
     * What thread group should be used for internal threads.
     */
    private ThreadGroup threadGroup;

    /**
     * What compressor to use for messages. Defaults to no compression.
     */
    private Compressor compressor;

    /**
     * Whether Accordion has been started.
     */
    private boolean started = false;
    /**
     * Whether automatic peer discovery should be performed through plugin channels.
     */
    @Getter(AccessLevel.PACKAGE) private boolean automaticDiscovery = true;

    AccordionApi() {
        externalAddress = listenAddress = InetAddress.getLoopbackAddress();
    }

    /**
     * Create a generic AccordionApi instance without automatic discovery or autostart.
     */
    public static AccordionApi create() {
        return new AccordionApi()
                .autoStart(false)
                .automaticDiscovery(false)
                .logger(LoggerFactory.getLogger(AccordionApi.class))
                .tier(DEFAULT_TIER_BUKKIT)
                .listen(false);
    }

    /**
     * Address this server is available under from all other nodes.
     */
    public AccordionApi externalAddress(InetAddress externalAddress) {
        checkNotStarted();
        this.externalAddress = externalAddress;
        return this;
    }

    /**
     * Interface this server should listen on if it is a "hub" node, something like "127.0.0.1" or "0.0.0.0".
     */
    public AccordionApi listenAddress(InetAddress listenAddress) {
        checkNotStarted();
        this.listenAddress = listenAddress;
        return this;
    }

    /**
     * Whether to autostart this server after all plugins are loaded.
     */
    public AccordionApi autoStart(boolean autoStart) {
        checkNotStarted();
        this.autoStart = autoStart;
        return this;
    }

    /**
     * Whether we should listen for remote connections.
     */
    public AccordionApi listen(boolean listen) {
        checkNotStarted();
        this.listen = listen;
        return this;
    }

    /**
     * The tier / group of this server.
     */
    public AccordionApi tier(int tier) {
        checkNotStarted();
        this.tier = tier;
        return this;
    }

    /**
     * The port our listener should operate on if we are a "hub" node.
     */
    public AccordionApi port(int port) {
        checkNotStarted();
        this.port = port;
        return this;
    }

    /**
     * Helper method that generates a port for the given minecraft listen port.
     */
    AccordionApi mcPort(int mcPort) {
        return port(mcPort + DEFAULT_MINECRAFT_PORT_OFFSET);
    }

    /**
     * Our logger.
     */
    public AccordionApi logger(Logger logger) {
        checkNotStarted();
        this.logger = logger;
        return this;
    }

    /**
     * Our logger.
     */
    public AccordionApi logger(java.util.logging.Logger logger) {
        return logger(JavaLogging.fromJavaLogger(logger));
    }

    /**
     * Whether automatic peer discovery should be performed through plugin channels.
     */
    public AccordionApi automaticDiscovery(boolean automaticDiscovery) {
        // this is a valid op even when started, no check
        this.automaticDiscovery = automaticDiscovery;
        return this;
    }

    /**
     * ConnectionListener specification used to build the network.
     */
    public AccordionApi connectionListener(ConnectionListenerFactory connectionListenerFactory) {
        checkNotStarted();
        this.connectionListenerFactory = connectionListenerFactory;
        return this;
    }

    /**
     * What thread group should be used for internal threads.
     */
    public AccordionApi threadGroup(ThreadGroup threadGroup) {
        checkNotStarted();
        this.threadGroup = threadGroup;
        return this;
    }

    /**
     * What compressor to use for messages. Defaults to no compression.
     */
    public AccordionApi compressor(Compressor compressor) {
        checkNotStarted();
        this.compressor = compressor;
        return this;
    }

    /**
     * Throws an exception if our server is started and basic configuration may not be performed anymore.
     */
    private synchronized void checkNotStarted() {
        Preconditions.checkState(!started, "Already started");
    }

    /**
     * Try to autostart this server.
     */
    void tryAutoStart() {
        if (autoStart && !started) {
            start();
        }
    }

    /**
     * Start this accordion instance.
     */
    public synchronized void start() {
        // synchronized so it doesn't get called twice

        checkNotStarted();
        started = true;
        // create identity
        Node self = new Node(new InetSocketAddress(externalAddress, port), tier);
        // build local node
        LocalNodeBuilder builder = LocalNode.builder();
        if (this.threadGroup != null) {
            builder.threadGroup(threadGroup);
        }
        if (this.compressor != null) {
            builder.compressor(compressor);
        }
        localNode = builder
                .logger(logger)
                .self(self)
                .listenAddress(new InetSocketAddress(listenAddress, port))
                .connectionListener(connectionListenerFactory)
                .build();
        // build packet channel
        channel = MessengerPacketChannel.create(localNode.getConnectionManager());
        // listen
        if (listen) {
            localNode.listen();
        }
    }
}
