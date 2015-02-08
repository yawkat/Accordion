/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.distributed;

import at.yawk.accordion.netty.Connection;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Interface that represents a collection that is synchronized between different nodes in the network.
 *
 * @author yawkat
 */
interface CollectionSynchronizer<T> {
    /**
     * Add an entry to this synchronizer.
     *
     * @return The added entry or an empty optional if this entry was already known.
     */
    default Optional<T> add(T newEntry) {
        return add(Stream.of(newEntry)).stream().findAny();
    }

    /**
     * Add a stream of entries to this synchronizer.
     *
     * @return A set of entries that were actually added (excluding ones that were already known).
     */
    Set<T> add(Stream<T> newEntries);

    /**
     * Add all entries encoded in the given message using #encode.
     *
     * @return A set of entries that were actually added (excluding ones that were already known).
     */
    Set<T> decodeAndAdd(ByteBuf message);

    /**
     * Encode all entries of this synchronizer into a ByteBuf that can be read by #decodeAndAdd.
     */
    ByteBuf encode();

    /**
     * Must be called when a node connects / we connect to a node. Ensures synchronization with that node.
     */
    void onConnected(Connection connection);

    /**
     * Get all known entries of this synchronizer.
     */
    Set<T> getEntries();
}
