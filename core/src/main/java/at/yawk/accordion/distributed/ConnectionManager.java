package at.yawk.accordion.distributed;

import at.yawk.accordion.Channel;
import at.yawk.accordion.Log;
import at.yawk.accordion.Messenger;
import at.yawk.accordion.codec.ByteCodec;
import at.yawk.accordion.compression.Compressor;
import at.yawk.accordion.compression.VoidCompressor;
import at.yawk.accordion.netty.Connection;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.Getter;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class for managing connections with other nodes, packet distribution and packet reading.
 *
 * @author yawkat
 */
public class ConnectionManager implements Messenger<ByteBuf> {
    /**
     * Maximum length a channel name may have.
     */
    private static final int MAX_CHANNEL_NAME_LENGTH = 0xFF;
    /**
     * RNG for generating random packet IDs.
     */
    private static final Random PACKET_ID_GENERATOR = new Random();

    private static final AtomicInteger threadId = new AtomicInteger();

    /**
     * Thread group used for all executors used by this ConnectionManager.
     */
    @Getter
    private final ThreadGroup threadGroup;

    /**
     * Our logger.
     */
    @Getter
    private final Logger logger;

    @Getter(AccessLevel.PACKAGE)
    private final Collection<Connection> connections = new CopyOnWriteArraySet<>();

    /**
     * Listener to be called when a connection dies. To add multiple listeners simply use Consumer.andThen to link
     * them.
     */
    private Consumer<Connection> disconnectListener;

    /**
     * Subscribers by channel name.
     */
    private final Map<String, Collection<Consumer<ByteBuf>>> listeners = new ConcurrentHashMap<>();
    /**
     * PacketDistinctionHandler to avoid duplicate packet handling.
     */
    private final PacketDistinctionHandler packetDistinctionHandler;

    /**
     * All channels in the network. Also the channels we need to receive to forward them to other nodes.
     */
    private final GraphCollectionSynchronizer<String> subscribedChannels;
    /**
     * Manages heartbeats and disconnects on timeout.
     */
    private final HeartbeatManager heartbeatManager;

    /**
     * Internal handlers for specific channels. If an internal handler for a channel is defined, it cannot be used for
     * normal communication. Packets in that channel will also not be forwarded to other servers.
     */
    private final Map<String, BiConsumer<ByteBuf, Connection>> internalHandlers = new HashMap<>();

    /**
     * Counter that gets incremented each time a new unique packet is received.
     */
    private final AtomicLong receivedPacketCount = new AtomicLong();
    /**
     * Counter that gets incremented each time a packet is received, including duplicate packets.
     */
    private final AtomicLong receivedPacketCountIncludingDuplicates = new AtomicLong();

    /**
     * Executor used for asynchronous connection writing.
     */
    private final Executor executor;

    /**
     * Compressor used to compress data between nodes. Note that there is no check to ensure two nodes use the same
     * compression.
     */
    private final Compressor compressor;

    private ConnectionManager(ThreadGroup threadGroup, Logger logger, Compressor compressor) {
        this.threadGroup = threadGroup;
        this.logger = logger;
        this.compressor = compressor;

        packetDistinctionHandler = PacketDistinctionHandler.createAndStart(threadGroup);
        executor = Executors
                .newCachedThreadPool(r -> new Thread(threadGroup,
                        r,
                        "Accordion handler thread #" + threadId.incrementAndGet()));

        // remove on disconnect.
        this.disconnectListener = connections::remove;

        subscribedChannels = new GraphCollectionSynchronizer<String>(this, InternalProtocol.SUBSCRIBE,
                new ByteCodec<String>() {
                    // normal string encode / decode
                    @Override
                    public String decode(ByteBuf encoded) {
                        return InternalProtocol.readByteString(
                                encoded);
                    }

                    @Override
                    public void encode(ByteBuf target,
                                       String message) {
                        InternalProtocol.writeByteString(target,
                                message);
                    }
                }) {
            @Override
            protected Set<String> handleUpdate(Set<String> newEntries, Connection origin) {
                Log.debug(getLogger(), () -> origin + " now subscribed to " + newEntries);
                return super.handleUpdate(newEntries, origin);
            }
        };

        heartbeatManager = new HeartbeatManager(this);
        heartbeatManager.start();
    }

    /**
     * Add a new internal handler.
     */
    void setInternalHandler(String channel, BiConsumer<ByteBuf, Connection> handler) {
        internalHandlers.put(channel, handler);
    }

    public static ConnectionManager create(ThreadGroup threadGroup, Logger logger) {
        return create(threadGroup, logger, VoidCompressor.getInstance());
    }

    public static ConnectionManager create(ThreadGroup threadGroup, Logger logger, Compressor compressor) {
        return new ConnectionManager(threadGroup, logger, compressor);
    }

    public static ConnectionManager create(Logger logger) {
        return create(logger, VoidCompressor.getInstance());
    }

    public static ConnectionManager create(Logger logger, Compressor compressor) {
        ThreadGroup group = null;
        SecurityManager sec = System.getSecurityManager();
        // check if security wants us to use a specific ThreadGroup
        if (sec != null) {
            group = sec.getThreadGroup();
        }
        if (group == null) {
            // default to parent group
            group = Thread.currentThread().getThreadGroup();
        }
        return create(group, logger, compressor);
    }

    public static ConnectionManager create() {
        return create(Log.getDefaultLogger());
    }

    /**
     * Add a new connection to this ConnectionManager. This connection will its handlers set: you cannot use it for
     * another ConnectionManager.
     */
    public void addConnection(Connection connection) {
        // wrap in async connection to avoid long blocking
        doAddConnection(new AsynchronousConnection(connection, executor));
    }

    private void doAddConnection(Connection connection) {
        connections.add(connection);

        connection.setDisconnectHandler(() -> disconnectListener.accept(connection));
        connection.setExceptionHandler(error -> {
            logger.error("Connection lost to " + connection, error);
            connection.disconnect();
        });
        // on receive
        connection.setMessageHandler(message -> handleRawMessage(connection, message));
        subscribedChannels.onConnected(connection);
        heartbeatManager.onConnected(connection);
    }

    /**
     * Handle a raw (encoded) message from the given connection.
     */
    private void handleRawMessage(Connection connection, ByteBuf message) {
        int startIndex = message.readerIndex();

        receivedPacketCountIncludingDuplicates.incrementAndGet();

        // read packet ID
        long packetId = message.readLong();
        if (!packetDistinctionHandler.register(packetId)) {
            // already received, do not handle again
            Log.debug(logger, () -> "Duplicate packet " + packetId);
            return;
        }

        receivedPacketCount.incrementAndGet();

        Stream<Connection> forwards = handleDecodedMessage(connection, compressor.decode(message), packetId);

        // reset reader index so we can copy the message
        message.readerIndex(startIndex);
        // forward packet to other connections that listen to this channel
        forwards
                // except the origin of the packet (they already got it)
                .filter(other -> other != connection)
                        // send
                .forEach(other -> copyAndSend(other, message));
    }

    /**
     * Handle a decoded message from the given connection.
     *
     * @return a stream of connections the message should be forwarded to.
     */
    private Stream<Connection> handleDecodedMessage(Connection sender, ByteBuf decoded, long id) {
        // read channel name
        String channelName = InternalProtocol.readByteString(decoded);

        Log.debug(logger,
                () -> "Received packet " + Long.toHexString(id) + " in channel '" + channelName + "' (" +
                        decoded.readableBytes() + " bytes)");

        // handle internally
        BiConsumer<ByteBuf, Connection> internalHandler = internalHandlers.get(channelName);
        if (internalHandler != null) {
            internalHandler.accept(decoded, sender);
            // internally handled, do not handle in user code or forward
            return Stream.empty();
        }

        // handle payload in listeners
        Collection<Consumer<ByteBuf>> subs = listeners.getOrDefault(channelName, Collections.emptySet());
        if (!subs.isEmpty()) {
            subs.forEach(listener -> listener.accept(decoded.copy()));
        }
        return getConnectionsSubscribedTo(channelName);
    }

    /**
     * Send a packet to the given connections.
     *
     * @param channel   The encoded channel this packet should be sent on.
     * @param receivers The connections it should be forwarded to.
     * @param payload   The payload of the packet that will be received by the other nodes.
     */
    void sendPacket(byte[] channel, Stream<Connection> receivers, ByteBuf payload) {
        long packetId = generateUniqueId();

        if (Log.isDebug(logger)) {
            List<Connection> connectionList = receivers.collect(Collectors.toList());
            logger.debug("Transmitting packet " + Long.toHexString(packetId) + " in channel '" + new String(channel) +
                    "' (" + payload.readableBytes() + " bytes) to " + connectionList);
            receivers = connectionList.stream();
        }
        // encode
        ByteBuf full = InternalProtocol.encodePacket(channel, packetId, payload, compressor);

        // transmit to all given connections
        receivers.forEach(connection -> copyAndSend(connection, full));
    }

    /**
     * Send a raw packet (with header fields already included) to the given connection. The given ByteBuf will not be
     * modified.
     */
    private void copyAndSend(Connection connection, ByteBuf full) {
        ByteBuf copy = full.copy();
        // 8 for packet ID, at least 1 for channel name or we're doing something wrong
        assert copy.readableBytes() > 9 : Arrays.toString(copy.array());
        connection.send(copy);
    }

    /**
     * Get a Set of all channels that connection is subscribed to.
     */
    private Set<String> getSubscribedChannels(Connection connection) {
        return subscribedChannels.getTheirEntries(connection);
    }

    /**
     * Get all connections subscribed to a given channel.
     */
    private Stream<Connection> getConnectionsSubscribedTo(String channelName) {
        return connections.parallelStream()
                // where subscribed
                .filter(connection -> getSubscribedChannels(connection).contains(channelName));
    }

    /**
     * Add a listener to be called when a connection dies.
     */
    public void addDisconnectListener(Consumer<Connection> onDisconnected) {
        disconnectListener = disconnectListener.andThen(onDisconnected);
    }

    /**
     * Get a channel implementation by name.
     */
    @Override
    public Channel<ByteBuf> getChannel(String name) {
        // encode
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        if (nameBytes.length > MAX_CHANNEL_NAME_LENGTH) {
            throw new UnsupportedOperationException("Maximum channel name length is " + MAX_CHANNEL_NAME_LENGTH);
        }

        return new Channel<ByteBuf>() {
            @Override
            public void publish(ByteBuf message) {
                // send
                sendPacket(nameBytes, getConnectionsSubscribedTo(name), message);
            }

            @Override
            public void subscribe(Consumer<ByteBuf> listener) {
                // listen
                subscribedChannels.add(name);
                listeners.computeIfAbsent(name, key -> new CopyOnWriteArrayList<>()).add(listener);
            }
        };
    }

    private long generateUniqueId() {
        // find a unique packet ID
        long packetId;
        do {
            packetId = PACKET_ID_GENERATOR.nextLong();
            // this loop isn't much slower than just assuming the ID is unused so we might as well check for
            // uniqueness, no matter how unlikely a collision is
        } while (!packetDistinctionHandler.register(packetId));

        return packetId;
    }

    /**
     * @see #receivedPacketCount
     */
    public long getReceivedPacketCount() {
        return receivedPacketCount.get();
    }

    /**
     * @see #receivedPacketCountIncludingDuplicates
     */
    public long getReceivedPacketCountIncludingDuplicates() {
        return receivedPacketCountIncludingDuplicates.get();
    }
}
