package at.yawk.accordion.distributed;

import at.yawk.accordion.codec.ByteCodec;
import at.yawk.accordion.netty.Connection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CollectionSynchronizer that provides the individual entry sets of connected nodes and the nodes behind them.
 * <p/>
 * <p/> 4 nodes are connected in this fashion:
 * <p/>
 * <code> A---B---C---D </code>
 * <p/>
 * <ul> <li>Let <code>A</code> have the entries <code>{1, 2}</code></li> <li>Let <code>B</code> have the entries
 * <code>{2, 3}</code></li> <li>Let <code>C</code> have the entries <code>{3, 4}</code></li> <li>Let <code>D</code> have
 * the entries <code>{4, 5}</code></li> </ul>
 * <p/>
 * Node <code>C</code> has two connections (to <code>A</code> and to <code>B</code>) and can get the entries behind
 * either: The entries behind <code>B</code> are <code>{1, 2, 3}</code> (because this includes <code>A</code> <i>and</i>
 * <code>B</code>), the entries in direction <code>D</code> are <code>{4, 5}</code>.
 * <p/>
 * <p/> This synchronizer is used to synchronize subscription states: A node only needs to receive messages in a channel
 * if they or a node behind them listen to it.
 *
 * @author yawkat
 */
class GraphCollectionSynchronizer<T> extends AbstractCollectionSynchronizer<T> {
    /**
     * The Connection property that contains a Set of entries that are behind a connection.
     */
    private static final String PROPERTY_THEIR_ENTRIES = ".entries";
    /**
     * The Connection property that contains a Set of entries that we told them that lied behind us.
     */
    private static final String PROPERTY_TRANSMITTED_ENTRIES = ".transmitted";

    /**
     * Our own entries, excluding the ones behind us.
     */
    private final Set<T> ourEntries = newConcurrentSet();

    GraphCollectionSynchronizer(ConnectionManager connectionManager, String channel, ByteCodec<T> serializer) {
        super(connectionManager, channel, serializer);
    }

    /**
     * Get a property of a connection by name, specific to this synchronizer. The property name consists of our channel
     * name and the given property name.
     */
    @SuppressWarnings("unchecked")
    private <O> O getChannelProperty(String name, Connection connection, Supplier<O> defaultSupplier) {
        return (O) connection.properties().computeIfAbsent(getChannel() + name, k -> defaultSupplier.get());
    }

    /**
     * Get a mutable set of entries that lie behind the given connection.
     */
    private Set<T> getTheirEntriesMut(Connection connection) {
        return getChannelProperty(PROPERTY_THEIR_ENTRIES, connection, GraphCollectionSynchronizer::newConcurrentSet);
    }

    /**
     * Get a set of entries that lie behind the given connection.
     */
    public Set<T> getTheirEntries(Connection connection) {
        return Collections.unmodifiableSet(getTheirEntriesMut(connection));
    }

    /**
     * Get a mutable set of entries that we have sent to the given connection.
     */
    private Set<T> getTransmittedEntries(Connection connection) {
        return getChannelProperty(PROPERTY_TRANSMITTED_ENTRIES, connection, GraphCollectionSynchronizer::newConcurrentSet);
    }

    /**
     * A stream of entries that either we or any connected node in the given predicate is subscribed to.
     */
    private Stream<T> globalEntries(Predicate<Connection> connectionPredicate) {
        Stream<T> others = getConnectionManager().getConnections().stream()
                .filter(connectionPredicate)
                .flatMap(connection -> getTheirEntriesMut(connection).stream());
        return Stream.concat(ourEntries.stream(), others).distinct();
    }

    @Override
    protected Set<T> handleUpdate(Set<T> newEntries, Connection origin) {
        // add to their entries
        getTheirEntriesMut(origin).addAll(newEntries);
        // update all connections except origin
        updateRemotes(getConnectionManager().getConnections().stream().filter(connection -> connection != origin));
        return newEntries;
    }

    @Override
    public Set<T> add(Stream<T> newEntries) {
        // compute set of entries added to ourEntries
        Set<T> added = new HashSet<>();
        newEntries.forEach(entry -> {
            boolean isNew = ourEntries.add(entry);
            if (isNew) {
                added.add(entry);
            }
        });
        // if we added anything, transmit changes.
        if (!added.isEmpty()) {
            updateAllRemotes();
        }
        return added;
    }

    @Override
    public void onConnected(Connection connection) {
        // update this connection (send it our entries and connected entries)
        updateRemotes(Stream.of(connection));
    }

    @Override
    public Set<T> getEntries() {
        return Collections.unmodifiableSet(ourEntries);
    }

    /**
     * Update the collections of all connected remotes.
     */
    private void updateAllRemotes() {
        updateRemotes(getConnectionManager().getConnections().stream());
    }

    /**
     * Update the collections of all given remotes.
     */
    private void updateRemotes(Stream<Connection> connections) {
        connections.forEach(connection -> {
            Set<T> has = getTransmittedEntries(connection);
            /*
             * Send all entries that
             * - we didn't send before
             * - either us or any connection apart from the target connection owns
             */
            Set<T> sending = globalEntries(other -> other != connection)
                    .filter(entry -> !has.contains(entry)).collect(Collectors.toSet());
            // transmit
            sendAdded(Stream.of(connection), sending);
            getTransmittedEntries(connection).addAll(sending);
        });
    }

    private static <T> Set<T> newConcurrentSet() {
        return Collections.newSetFromMap(new ConcurrentHashMap<>());
    }
}
