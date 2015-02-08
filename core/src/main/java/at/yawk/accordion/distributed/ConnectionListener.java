/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.distributed;

import java.util.Optional;
import java.util.Set;

/**
 * Listener for connection changes in #LocalNode.
 *
 * @author yawkat
 */
public interface ConnectionListener {
    /**
     * Called when a connection to another node is established but no handshake was received yet.
     *
     * @param other        If we know the identity of the remote node, that node, an empty Optional otherwise.
     * @param thisIsServer true if this connection was established with us as the server.
     */
    default void preConnected(Optional<Node> other, boolean thisIsServer) {}

    /**
     * Called when a handshake was received by a remote server.
     *
     * @param other        The identity of the newly connected node.
     * @param thisIsServer true if this connection was established with us as the server.
     */
    default void connected(Node other, boolean thisIsServer) {}

    /**
     * Called when new nodes are registered.
     *
     * @param newNodes            The now known nodes.
     * @param fromSynchronization true if these nodes were added through global node synchronization, false if they were
     *                            added programmatically.
     */
    default void nodesRegistered(Set<Node> newNodes, boolean fromSynchronization) {}

    /**
     * Called when a node disconnected from us.
     *
     * @param other The node that disconnected.
     */
    default void disconnected(Node other) {}

    /**
     * Called when a connection attempt to a remote node failed.
     */
    default void connectionAttemptFailed(Node other) {}
}
