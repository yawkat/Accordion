package at.yawk.accordion.distributed;

import at.yawk.accordion.Log;
import at.yawk.accordion.netty.NettyConnector;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * Builder class for LocalNode.
 *
 * @author yawkat
 */
public class LocalNodeBuilder {
    private Optional<Logger> logger = Optional.empty();
    private Optional<SocketAddress> listenAddress = Optional.empty();
    private Optional<Node> self = Optional.empty();
    private Optional<ConnectionListenerFactory> connectionListenerFactory = Optional.empty();
    private Optional<ThreadGroup> threadGroup = Optional.empty();

    public LocalNodeBuilder() {}

    /**
     * What logger we should use. Defaults to Log.getDefaultLogger.
     */
    public LocalNodeBuilder logger(Logger logger) {
        this.logger = Optional.of(logger);
        return this;
    }

    /**
     * Address our server should listen to. Defaults to the external port given in #self and 0.0.0.0 as interface.
     */
    public LocalNodeBuilder listenAddress(SocketAddress listenAddress) {
        this.listenAddress = Optional.of(listenAddress);
        return this;
    }

    /**
     * Set our own identity.
     */
    public LocalNodeBuilder self(Node self) {
        this.self = Optional.of(self);
        return this;
    }

    /**
     * Set a ConnectionListenerFactory to use for handling our LocalNode.
     */
    public LocalNodeBuilder connectionListener(ConnectionListenerFactory connectionListenerFactory) {
        this.connectionListenerFactory = Optional.of(connectionListenerFactory);
        return this;
    }

    /**
     * What thread group should be used for internal threads.
     */
    public LocalNodeBuilder threadGroup(ThreadGroup threadGroup) {
        this.threadGroup = Optional.of(threadGroup);
        return this;
    }

    /**
     * Build this LocalNode.
     */
    public LocalNode build() {
        Logger logger = this.logger.orElseGet(Log::getDefaultLogger);
        ConnectionManager connectionManager = threadGroup.isPresent() ?
                ConnectionManager.create(threadGroup.get(), logger) :
                ConnectionManager.create(logger);
        // self is required
        Node self = this.self.orElseThrow(() -> new IllegalStateException("self must be set"));
        SocketAddress listenAddress = this.listenAddress.orElseGet(() -> {
            try {
                // listen to 0.0.0.0
                return new InetSocketAddress(InetAddress.getByAddress(new byte[4]),
                                             self.getAddress().getPort());
            } catch (UnknownHostException e) {
                // 0.0.0.0 should always be valid
                throw new RuntimeException(e);
            }
        });
        ConnectionListenerFactory connectionListenerFactory =
                this.connectionListenerFactory.orElse(NopConnectionListener::getInstance);
        return new LocalNode(connectionManager,
                             NettyConnector.getInstance(),
                             self,
                             listenAddress,
                             connectionListenerFactory);
    }
}
