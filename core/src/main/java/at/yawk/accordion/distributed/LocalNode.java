package at.yawk.accordion.distributed;

import at.yawk.accordion.Log;
import at.yawk.accordion.netty.Connection;
import at.yawk.accordion.netty.Connector;
import at.yawk.accordion.netty.Server;
import io.netty.buffer.ByteBuf;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import lombok.Getter;
import org.slf4j.Logger;

/**
 * High-level wrapper on top of ConnectionManager that automatically creates and sustains a network with other nodes.
 *
 * @author yawkat
 */
public class LocalNode {
    /**
     * Connection property used to indicate whether a Connection is initialized with us as the server.
     */
    private static final String PROPERTY_IS_THIS_SERVER = "isThisServer";

    /**
     * Our connection manager.
     */
    @Getter private final ConnectionManager connectionManager;
    /**
     * Connector used to spawn new Connections.
     */
    private final Connector connector;

    /**
     * Our node entry that we should send to other servers so they know who we are.
     */
    @Getter private final Node self;
    /**
     * Address our server should listen at when running.
     */
    private final SocketAddress listenerAddress;
    /**
     * A distributed set of all known nodes (including offline ones).
     */
    private final CollectionSynchronizer<Node> remoteNodes;

    /**
     * Map of connected nodes to their respective connections.
     */
    private final Map<Node, Connection> nodesToConnections = new ConcurrentHashMap<>();
    /**
     * Map of connections to their respective nodes.
     */
    private final Map<Connection, Node> connectionsToNodes = new ConcurrentHashMap<>();

    /**
     * Connection listener that is called on connection updates.
     */
    private final ConnectionListener connectionListener;

    LocalNode(ConnectionManager connectionManager,
              Connector connector,
              Node self,
              SocketAddress listenerAddress,
              ConnectionListenerFactory connectionListenerFactory) {

        this.connectionManager = connectionManager;
        this.connector = connector;
        this.self = self;
        this.listenerAddress = listenerAddress;
        this.connectionListener = connectionListenerFactory.createConnectionListener(this);

        remoteNodes = new BasicCollectionSynchronizer<Node>(this.connectionManager,
                                                            InternalProtocol.SYNC_NODES,
                                                            Node.getCodec()) {
            @Override
            protected Set<Node> handleUpdate(Set<Node> newEntries, Connection origin) {
                // call our listener with the newly known nodes.
                Set<Node> added = super.handleUpdate(newEntries, origin);
                // check if we knew all nodes already.
                if (!added.isEmpty()) {
                    connectionListener.nodesRegistered(added, true);
                }
                return added;
            }
        };

        // handler for handshake so we know our peers.
        connectionManager.setInternalHandler(InternalProtocol.WELCOME, (message, connection) -> {
            // Read and add node
            Node node = Node.read(message);
            // already connected to that node
            if (nodesToConnections.containsKey(node)) {
                connection.disconnect();
                return;
            }

            nodesToConnections.put(node, connection);
            connectionsToNodes.put(connection, node);
            Log.info(getLogger(), () -> node + " connected");
            // notify listener
            connectionListener.connected(node, (Boolean) connection.properties().getOrDefault(PROPERTY_IS_THIS_SERVER,
                                                                                              true));
        });
        // unregister & notify listener on disconnect
        connectionManager.addDisconnectListener(connection -> {
            Node node = connectionsToNodes.remove(connection);
            if (node != null) {
                nodesToConnections.remove(node);
                connectionListener.disconnected(node);
            }
        });
    }

    public static LocalNodeBuilder builder() {
        return new LocalNodeBuilder();
    }

    /**
     * Start our server to listen for new nodes.
     */
    public void listen() {
        Log.info(getLogger(), () -> "Listening on " + listenerAddress);
        // our server
        Server server = connector.listen(listenerAddress);
        server.setConnectionHandler(t -> {
            // listen for new connections
            t.properties().put(PROPERTY_IS_THIS_SERVER, true);
            Log.info(getLogger(), () -> "Peer Connected");
            addConnection(t);
            connectionListener.preConnected(Optional.empty(), true);
        });
        // bind
        server.bind();
    }

    /**
     * Add a new connection and send handshake.
     */
    private void addConnection(Connection connection) {
        Log.debug(getLogger(), () -> "Adding connection " + connection);
        connectionManager.addConnection(connection);
        remoteNodes.onConnected(connection);

        // handshake
        connectionManager.sendPacket(InternalProtocol.WELCOME_BYTES,
                                     Stream.of(connection),
                                     Node.getCodec().encode(self));
    }

    /**
     * Attempt to connect to the given node.
     */
    public void connect(Node other) {
        Optional<Connection> op = connector.connect(other.getAddress());
        if (op.isPresent()) {
            // successful
            op.get().properties().put(PROPERTY_IS_THIS_SERVER, false);
            addConnection(op.get());
            connectionListener.preConnected(Optional.of(other), false);
            Log.info(getLogger(), () -> "Connected to " + other);
        } else {
            // failed
            connectionListener.connectionAttemptFailed(other);
            Log.info(getLogger(), () -> "Failed to connect to " + other);
        }
    }

    /**
     * Add the given nodes so we can connect to them.
     */
    public void addNodes(Stream<Node> remoteNodes) {
        Set<Node> added = this.remoteNodes.add(remoteNodes);
        connectionListener.nodesRegistered(added, false);
    }

    public Logger getLogger() {
        return connectionManager.getLogger();
    }

    /**
     * Get all nodes we know about.
     */
    public Set<Node> getKnownNodes() {
        return remoteNodes.getEntries();
    }

    /**
     * Get all nodes we are currently connected to.
     */
    public Set<Node> getConnectedNodes() {
        return Collections.unmodifiableSet(nodesToConnections.keySet());
    }

    /**
     * Get an encoded representation of all nodes we know about.
     */
    public ByteBuf getKnownNodesEncoded() {
        return remoteNodes.encode();
    }

    /**
     * Load a set of encoded nodes encoded via #getKnownNodesEncoded.
     */
    public void loadEncodedNodes(ByteBuf encodedNodes) {
        Set<Node> newNodes = remoteNodes.decodeAndAdd(encodedNodes);
        connectionListener.nodesRegistered(newNodes, false);
    }
}
