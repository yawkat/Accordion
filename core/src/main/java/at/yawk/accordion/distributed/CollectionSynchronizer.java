package at.yawk.accordion.distributed;

import at.yawk.accordion.Log;
import at.yawk.accordion.codec.ByteCodec;
import at.yawk.accordion.netty.Connection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a Set that is synchronized across a network of Nodes. When entries are added to this set the change will
 * reflect across the network. It is not possible to remove entries from this set.
 *
 * This class is used to synchronize node lists and subscribed channels.
 *
 * @author yawkat
 */
class CollectionSynchronizer<T> {
    /**
     * The locally known entries of this set.
     */
    private final Set<T> entries = new HashSet<>();

    /**
     * The underlying ConnectionManager instance.
     */
    private final ConnectionManager connectionManager;
    /**
     * The channel name reserved for this synchronizer.
     */
    private final String channel;
    /**
     * The encoded channel name of this synchronizer.
     */
    private final byte[] channelNameBytes;
    /**
     * The ByteCodec used to serialize and deserialize entries of this set.
     */
    private final ByteCodec<T> serializer;

    CollectionSynchronizer(ConnectionManager connectionManager, String channel, ByteCodec<T> serializer) {
        this.connectionManager = connectionManager;
        this.channel = channel;
        this.channelNameBytes = channel.getBytes(StandardCharsets.UTF_8);
        this.serializer = serializer;

        // reserve our channel in the connection manager
        connectionManager.setInternalHandler(channel, (message, origin) -> {
            // received new entries from origin
            Set<T> entries = decode(message);
            Log.debug(connectionManager.getLogger(), () -> channel + "<+ " + entries);
            handleUpdate(entries, origin);
        });
    }

    /**
     * Called when origin requests the addition of newEntries to the public set. This method can be overridden for event
     * handling.
     *
     * @return The set of entries that were actually added (excluding ones that we already knew about).
     */
    protected Set<T> handleUpdate(Set<T> newEntries, Connection origin) {
        return add(connection -> connection != origin, newEntries.stream());
    }

    /**
     * Add an entry to this synchronizer.
     *
     * @return The added entry or an empty optional if this entry was already known.
     */
    public Optional<T> add(T newEntry) {
        return add(Stream.of(newEntry)).stream().findAny();
    }

    /**
     * Add a stream of entries to this synchronizer.
     *
     * @return A set of entries that were actually added (excluding ones that were already known).
     */
    public Set<T> add(Stream<T> newEntries) {
        // send to all
        return add(con -> true, newEntries);
    }

    /**
     * Add a stream of entries to this synchronizer and only forward the changes to connections that match the given
     * predicate (maybe those that don't already know about these entries).
     *
     * @return A set of entries that were actually added (excluding ones that were already known).
     */
    private Set<T> add(Predicate<Connection> forwardPredicate, Stream<T> newEntries) {
        // add and compute previously unknown entries
        Set<T> added = new HashSet<>();
        newEntries.forEach(entry -> {
            if (entries.add(entry)) {
                // previously unknown
                added.add(entry);
            }
        });
        // update remotes
        sendAdded(forwardPredicate, added);
        return Collections.unmodifiableSet(added);
    }

    /**
     * Send the given entries to all connections that match the given predicate.
     */
    private void sendAdded(Predicate<Connection> forwardPredicate, Set<T> added) {
        sendAdded(connectionManager.getConnections().stream().filter(forwardPredicate), added);
    }

    /**
     * Send the given entries to the given connections.
     */
    private void sendAdded(Stream<Connection> targets, Set<T> added) {
        // no change, abort
        if (added.isEmpty()) {
            return;
        }
        // encode entries
        ByteBuf message = encode(added);
        if (Log.isDebug(connectionManager.getLogger())) {
            // dump targets into list so we can print them
            List<Connection> targetList = targets.collect(Collectors.toList());
            connectionManager.getLogger().debug(channel + ">+ " + added + " to " + targetList);
            // use list as target source because we drained the stream
            targets = targetList.stream();
        }
        // send the update packet
        connectionManager.sendPacket(channelNameBytes, targets, message);
    }

    /**
     * Add all entries encoded in the given message using #encode.
     *
     * @return A set of entries that were actually added (excluding ones that were already known).
     */
    public Set<T> decodeAndAdd(ByteBuf message) {
        return add(decode(message).stream());
    }

    /**
     * Decode the entries encoded by #encode.
     */
    private Set<T> decode(ByteBuf message) {
        // ushort length
        int count = message.readUnsignedShort();
        // read individual entries
        Set<T> entries = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(this.serializer.decode(message));
        }
        return entries;
    }

    /**
     * Encode all entries of this synchronizer into a ByteBuf that can be read by #decodeAndAdd.
     */
    public ByteBuf encode() {
        return encode(entries);
    }

    /**
     * Encode a set of entries into a bytebuf.
     */
    private ByteBuf encode(Set<T> added) {
        ByteBuf message = Unpooled.buffer();
        // ushort length
        message.writeShort(added.size());
        // individual entries written sequentially
        added.forEach(entry -> serializer.encode(message, entry));
        return message;
    }

    /**
     * Must be called when a node connects / we connect to a node. Ensures synchronization with that node.
     */
    public void onConnected(Connection connection) {
        sendAdded(Stream.of(connection), entries);
    }

    /**
     * Get all known entries of this synchronizer.
     */
    public Set<T> getEntries() {
        return Collections.unmodifiableSet(entries);
    }
}
