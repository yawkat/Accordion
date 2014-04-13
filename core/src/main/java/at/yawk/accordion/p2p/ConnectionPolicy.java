package at.yawk.accordion.p2p;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * A ConnectionPolicy defines how the network of nodes should look and how packets should be routed through it. For
 * example, a ConnectionPolicy could be implemented that routes all traffic through a single server. Another possibility
 * is a full-featured Peer-To-Peer system where each node is independent and outages have little impact.
 *
 * @author Yawkat
 */
public interface ConnectionPolicy {
    /**
     * Calculate routing of a packet.
     *
     * @param self           This node.
     * @param remote         The node we might be sending the packet to.
     * @param packetReceiver The RecipientRange that should have access to the packet.
     * @return true if the packet should be routed through the given remote, false otherwise.
     */
    boolean routePacketThrough(Recipient self, Recipient remote, RecipientRange packetReceiver);

    /**
     * Calculate the peers that self can connect to.
     *
     * @param self          This node.
     * @param possibilities The other suggested nodes. Never contains self or already connected nodes.
     * @return A sorted stream of nodes to connect to. They will be connected to in the Streams order.
     */
    Stream<Recipient> listPossiblePeers(Recipient self, Collection<Recipient> possibilities);

    /**
     * Calculates if a node should allow (more) incoming connections.
     *
     * @param self                 This node.
     * @param connectedToSelfCount The amount of incoming connections currently connected to this node.
     * @param selfConnectedToCount The amount of outgoing connections from this node.
     * @return true if the listener should be open, false otherwise.
     */
    boolean listenForPeers(Recipient self, int connectedToSelfCount, int selfConnectedToCount);

    /**
     * Calculates if a node should actively search for more peers to connect to.
     *
     * @param self                 This node.
     * @param connectedToSelfCount The amount of incoming connections currently connected to this node.
     * @param selfConnectedToCount The amount of outgoing connections from this node.
     * @return true if more nodes should be scanned, false otherwise.
     */
    boolean searchForPeers(Recipient self, int connectedToSelfCount, int selfConnectedToCount);
}
