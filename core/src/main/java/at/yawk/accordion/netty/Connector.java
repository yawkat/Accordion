package at.yawk.accordion.netty;

import java.net.SocketAddress;
import java.util.Optional;

/**
 * Interface for a networking system that allows connection to other servers and listening to a port.
 *
 * @author yawkat
 */
public interface Connector {
    /**
     * Connect to the given address. Returns an empty optional on failure, the connection otherwise.
     */
    Optional<Connection> connect(SocketAddress address);

    /**
     * Create a new server on the given address. This server is not started: call Server#bind to start it.
     */
    Server listen(SocketAddress inf);
}
