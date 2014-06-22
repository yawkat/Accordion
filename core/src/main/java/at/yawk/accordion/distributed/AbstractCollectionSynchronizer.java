package at.yawk.accordion.distributed;

import at.yawk.accordion.Log;
import at.yawk.accordion.codec.ByteCodec;
import at.yawk.accordion.netty.Connection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/**
 * Abstract CollectionSynchronizer implementation that handles sending, receiving, encoding and decoding entries for its
 * subclasses.
 *
 * @author yawkat
 */
abstract class AbstractCollectionSynchronizer<T> implements CollectionSynchronizer<T> {
    /**
     * The underlying ConnectionManager instance.
     */
    @Getter private final ConnectionManager connectionManager;
    /**
     * The channel name reserved for this synchronizer.
     */
    @Getter private final String channel;
    /**
     * The encoded channel name of this synchronizer.
     */
    private final byte[] channelNameBytes;
    /**
     * The ByteCodec used to serialize and deserialize entries of this set.
     */
    private final ByteCodec<T> serializer;

    protected AbstractCollectionSynchronizer(ConnectionManager connectionManager,
                                             String channel,
                                             ByteCodec<T> serializer) {
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
    protected abstract Set<T> handleUpdate(Set<T> newEntries, Connection origin);

    /**
     * Send the given entries to the given connections.
     */
    protected void sendAdded(Stream<Connection> targets, Set<T> added) {
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
    @Override
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

    @Override
    public ByteBuf encode() {
        return encode(getEntries());
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
}
