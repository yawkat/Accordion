package at.yawk.accordion.p2p;

import java.net.SocketAddress;

/**
 * Identification of a single node in our network.
 *
 * @author Yawkat
 */
public interface Recipient {
    /**
     * The address used to connect to this node.
     */
    SocketAddress getAddress();

    /**
     * The address this node should be bound as; Usually the same as #getAddress just with the host set to localhost.
     */
    SocketAddress getBindAddress();
}
