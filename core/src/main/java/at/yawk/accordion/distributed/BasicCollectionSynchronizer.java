/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.distributed;

import at.yawk.accordion.codec.ByteCodec;
import at.yawk.accordion.netty.Connection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Represents a Set that is synchronized across a network of Nodes. When entries are added to this set the change will
 * reflect across the network. It is not possible to remove entries from this set.
 *
 * This class is used to synchronize node lists.
 *
 * @author yawkat
 */
class BasicCollectionSynchronizer<T> extends AbstractCollectionSynchronizer<T> {
    /**
     * The locally known entries of this set.
     */
    private final Set<T> entries = Collections.synchronizedSet(new HashSet<>());

    BasicCollectionSynchronizer(ConnectionManager connectionManager, String channel, ByteCodec<T> serializer) {
        super(connectionManager, channel, serializer);
    }

    /**
     * Called when origin requests the addition of newEntries to the public set. This method can be overridden for event
     * handling.
     *
     * @return The set of entries that were actually added (excluding ones that we already knew about).
     */
    @Override
    protected Set<T> handleUpdate(Set<T> newEntries, Connection origin) {
        return add(connection -> connection != origin, newEntries.stream());
    }

    @Override
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
        sendAdded(getConnectionManager().getConnections().stream().filter(forwardPredicate), added);
    }

    @Override
    public void onConnected(Connection connection) {
        sendAdded(Stream.of(connection), entries);
    }

    @Override
    public Set<T> getEntries() {
        return Collections.unmodifiableSet(entries);
    }
}
