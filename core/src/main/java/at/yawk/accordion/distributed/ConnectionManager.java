package at.yawk.accordion.distributed;

import at.yawk.accordion.Channel;
import at.yawk.accordion.Log;
import at.yawk.accordion.Messenger;
import at.yawk.accordion.codec.ByteCodec;
import at.yawk.accordion.netty.Connection;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import org.slf4j.Logger;

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
     * Property used in Connection#properties for channels that connection has subscribed to.
     */
    private static final String PROPERTY_SUBSCRIBED = "subscribedChannels";
    /**
     * RNG for generating random packet IDs.
     */
    private static final Random PACKET_ID_GENERATOR = new Random();

    /**
     * Our logger.
     */
    @Getter private final Logger logger;

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
    private final PacketDistinctionHandler packetDistinctionHandler = PacketDistinctionHandler.createAndStart();

    /**
     * All channels in the network. Also the channels we need to receive to forward them to other nodes.
     */
    // TODO clean this collection up somehow so we don't receive all channels if we disconnect-reconnect
    private final CollectionSynchronizer<String> subscribedChannels;

    /**
     * Internal handlers for specific channels. If an internal handler for a channel is defined, it cannot be used for
     * normal communication. Packets in that channel will also not be forwarded to other servers.
     */
    private final Map<String, BiConsumer<ByteBuf, Connection>> internalHandlers = new HashMap<>();

    private ConnectionManager(Logger logger) {
        this.logger = logger;

        // remove on disconnect.
        this.disconnectListener = connections::remove;

        subscribedChannels = new CollectionSynchronizer<String>(this, InternalProtocol.SUBSCRIBE,
                                                                new ByteCodec<String>() {
                                                                    // normal string encode / decode
                                                                    @Override
                                                                    public String decode(ByteBuf encoded) {
                                                                        return InternalProtocol.readByteString(encoded);
                                                                    }

                                                                    @Override
                                                                    public void encode(ByteBuf target, String message) {
                                                                        InternalProtocol.writeByteString(target,
                                                                                                         message);
                                                                    }
                                                                }) {
            @Override
            protected Set<String> handleUpdate(Set<String> newEntries, Connection origin) {
                /*
                 * Hack to register channels that remote nodes listen to. If we receive new entries from a
                 * connection, we can be sure that either they or a node behind them listens to those channels: they
                 * would not send the newly subscribed channels back to us if we sent them to them. Because of this
                 * we can be certain that they actually want to listen to that channel (or a node behind them does in
                 * which case they need to forward it anyway).
                 */
                getSubscribedChannels(origin).addAll(newEntries);
                Log.debug(getLogger(), () -> origin + " now subscribed to " + newEntries);
                return super.handleUpdate(newEntries, origin);
            }
        };
    }

    /**
     * Add a new internal handler.s
     */
    void setInternalHandler(String channel, BiConsumer<ByteBuf, Connection> handler) {
        internalHandlers.put(channel, handler);
    }

    public static ConnectionManager create(Logger logger) {
        return new ConnectionManager(logger);
    }

    public static ConnectionManager create() {
        return create(Log.getDefaultLogger());
    }

    /**
     * Add a new connection to this ConnectionManager. This connection will its handlers set: you cannot use it for
     * another ConnectionManager.
     */
    public void addConnection(Connection connection) {
        connections.add(connection);

        connection.setDisconnectHandler(() -> disconnectListener.accept(connection));
        connection.setExceptionHandler(error -> {
            logger.error("Connection lost to " + connection, error);
            connection.disconnect();
        });
        // on receive
        connection.setMessageHandler(message -> {
            // read packet ID
            long packetId = message.readLong();
            if (!packetDistinctionHandler.register(packetId)) {
                // already received, do not handle again
                Log.debug(logger, () -> "Duplicate packet " + packetId);
                return;
            }

            // read channel name
            String channelName = InternalProtocol.readByteString(message);

            Log.debug(logger,
                      () -> "Received packet " + Long.toHexString(packetId) + " in channel '" + channelName + "' (" +
                            message.readableBytes() + " bytes)");

            // handle internally
            BiConsumer<ByteBuf, Connection> internalHandler = internalHandlers.get(channelName);
            if (internalHandler != null) {
                internalHandler.accept(message, connection);
                // internally handled, do not handle in user code or forward
                return;
            }

            // handle payload in listeners
            listeners.getOrDefault(channelName, Collections.emptySet())
                    .stream()
                    .forEach(listener -> listener.accept(message.copy()));

            // reset reader index so we can copy the message
            message.resetReaderIndex();
            // forward packet to other connections that listen to this channel
            getConnectionsSubscribedTo(channelName)
                    // except the origin of the packet (they already got it)
                    .filter(other -> other != connection)
                            // send
                    .forEach(other -> copyAndSend(other, message));
        });
        subscribedChannels.onConnected(connection);
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
        ByteBuf full = InternalProtocol.encodePacket(channel, packetId, payload);

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
     * Get a mutable Set of all channels that connection is subscribed to.
     */
    @SuppressWarnings("unchecked")
    private Set<String> getSubscribedChannels(Connection connection) {
        return (Set<String>) connection.properties().computeIfAbsent(PROPERTY_SUBSCRIBED,
                                                                     k -> Collections.synchronizedSet(new HashSet<>()));
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
}
