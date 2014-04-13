package at.yawk.accordion.p2p;

import at.yawk.accordion.bare.Direction;
import at.yawk.accordion.bare.NetworkManager;
import at.yawk.accordion.bare.Server;
import at.yawk.accordion.bare.Session;
import at.yawk.accordion.packet.PacketManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j;

/**
 * Manager for a decentral network of nodes.
 *
 * @author Yawkat
 */
@Log4j
public class P2PManager {
    /**
     * This node.
     */
    @Getter private final Recipient self;
    /**
     * The serializer / deserializer to create application-specific Recipients and RecipientRanges.
     */
    private final RecipientFactory recipientFactory;
    /**
     * The ConnectionPolicy of this network: Controls the structure of the network.
     */
    private final ConnectionPolicy connectionPolicy;

    /**
     * Internal executor used for EnvelopeRegistry garbage collection and connection handling.
     */
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    /**
     * Packet ID registry.
     */
    @Getter private final PacketManager packetManager = new PacketManager();
    /**
     * Low-level network manager.
     */
    private final NetworkManager networkManager;
    /**
     * The server singleton, created by NetworkManager.
     */
    private final Server server;

    /**
     * All other nodes. Connected nodes have a set, not-disconnected value.
     */
    private final Map<Recipient, Optional<Session>> sessions = new ConcurrentHashMap<>();
    /**
     * Amount of servers we are connected to.
     */
    private final AtomicInteger connectedToCount = new AtomicInteger(0);
    /**
     * Amount of clients that are connected to our server.
     */
    private final AtomicInteger connectedToSelfCount = new AtomicInteger(0);

    /**
     * The BoundServer singleton (created by #server).
     */
    @NonNull private Optional<Server.BoundServer> listener = Optional.empty();
    /**
     * The envelope registry used to avoid handling the same packet twice.
     */
    private final EnvelopeRegistry envelopeRegistry;
    /**
     * The message handler. Should be set by the application.
     */
    @Setter private Consumer<Envelope> messageHandler = e -> {};

    /**
     * @param self             This node.
     * @param connectionPolicy The ConnectionPolicy used to shape the network.
     * @param recipientFactory The RecipientFactory to serialize / deserialize Recipients.
     */
    public P2PManager(Recipient self, ConnectionPolicy connectionPolicy, RecipientFactory recipientFactory) {
        this.self = self;
        this.connectionPolicy = connectionPolicy;
        this.recipientFactory = recipientFactory;
        this.networkManager = new NetworkManager();
        this.server = networkManager.createServer();
        this.envelopeRegistry = new EnvelopeRegistry(executor);

        // assign handler.
        server.setSessionHandler(this::setupUnknownSession);
    }

    /**
     * Register a new peer. Allows connecting to it but does not perform that connection.
     */
    public void registerPeer(Recipient peer) {
        if (peer.equals(getSelf())) { return; }
        sessions.put(peer, Optional.empty());
    }

    /**
     * Start scanning and listening for peers according to our ConnectionPolicy.
     */
    public void start() {
        findPeersIfNecessaryAsync();
    }

    /**
     * Attempt to connect to the best peer decided by our ConnectionPolicy.
     */
    private void findAndConnectToPeer() {
        // the possible peers
        Stream<Recipient> possibilities = sessions.entrySet().stream().filter(e -> {
            Optional<Session> session = e.getValue();
            return !session.isPresent() || !session.get().isConnected();
        }).map(Map.Entry::getKey);

        // the peers decided by the ConnectionPolicy to be the most important.
        Stream<Recipient> prioritized = connectionPolicy.listPossiblePeers(getSelf(),
                                                                           possibilities.collect(Collectors.toSet()));

        // The peer we will attempt to connect to.
        Optional<Recipient> target = prioritized.findFirst();
        if (!target.isPresent()) { return; } // no more peers

        // Attempt connection
        Session session = networkManager.connect(target.get().getAddress());

        if (session == null) { return; } // connection failed

        // Handshake
        log.debug("Welcoming remote...");
        ByteBuf hello = Unpooled.buffer();
        recipientFactory.write(hello, getSelf());
        session.transmit(hello);

        // register

        Optional<Session> old = sessions.putIfAbsent(target.get(), Optional.of(session));

        if (old.isPresent()) {
            // race condition, revert
            session.disconnect();
        } else {
            setupSession(target.get(), session);
        }
    }

    /**
     * Start listening for peers.
     */
    private synchronized void bind() {
        if (listener.isPresent()) { return; }
        listener = Optional.of(server.bind(getSelf().getBindAddress()));
    }

    /**
     * Stop listening for peers.
     */
    private synchronized void unbind() {
        if (!listener.isPresent()) { return; }
        try {
            listener.get().close();
        } catch (IOException e) { log.error(e); }
        listener = Optional.empty();
    }

    /**
     * Asynchronous #findPeersIfNecessary.
     */
    private void findPeersIfNecessaryAsync() {
        executor.execute(this::findPeersIfNecessary);
    }

    /**
     * Updates bind / unbind status and searches for more nodes if necessary.
     */
    private void findPeersIfNecessary() {
        if (connectionPolicy.listenForPeers(getSelf(), connectedToSelfCount.get(), connectedToCount.get())) {
            bind();
        } else {
            unbind();
        }
        if (connectionPolicy.searchForPeers(getSelf(), connectedToSelfCount.get(), connectedToCount.get())) {
            findAndConnectToPeer();
            executor.schedule(this::findPeersIfNecessary, 500, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Setup a session of which we do not know the other end yet. Listens for identification.
     */
    private void setupUnknownSession(Session session) {
        session.setMessageHandler(b -> {
            Recipient remote = recipientFactory.read(b);
            setupSession(remote, session);
        });
    }

    /**
     * Set up and register a session of which we know the other end.
     */
    private void setupSession(Recipient remote, Session session) {
        session.setDestructionListener(() -> {
            // if this session is still modern, remove it (replace with empty optional)
            sessions.compute(remote, (k, v) -> {
                if (Optional.of(session).equals(v)) {
                    if (session.getDirection() == Direction.TO_CLIENT) {
                        connectedToCount.decrementAndGet();
                    } else {
                        connectedToSelfCount.decrementAndGet();
                    }
                    return Optional.empty();
                } else {
                    // session is outdated, do nothing
                    return v;
                }
            });
            findPeersIfNecessaryAsync();
        });
        session.setErrorHandler(t -> {
            log.error(t);
            session.disconnect();
        });
        session.setMessageHandler(t -> handleReceived(remote, t));
        // register
        sessions.put(remote, Optional.of(session));
        if (session.getDirection() == Direction.TO_CLIENT) {
            connectedToCount.incrementAndGet();
        } else {
            connectedToSelfCount.incrementAndGet();
        }
        log.info("Connected to peer " + remote + " (" + session.getDirection() + ")");
    }

    /**
     * Called when we received a packet from the given sender.
     */
    private void handleReceived(Recipient sender, ByteBuf buf) {
        // read id
        long id = buf.readLong();
        // check for duplicate packets
        if (!envelopeRegistry.register(id)) { return; }
        // parse
        RecipientRange recipientRange = recipientFactory.readRange(buf);
        ByteBuf payload = buf.readBytes(buf.readableBytes());
        Envelope envelope = Envelope.create(sender, getPacketManager(), id, recipientRange, payload);
        // spread across network (this is cool)
        transmit(envelope);
        // should we actually handle this packet?
        if (!recipientRange.contains(getSelf())) { return; }
        try {
            // handle
            messageHandler.accept(envelope);
        } catch (Throwable t) { log.error(t); }
    }

    /**
     * Transmit an envelope to all peers, according to our ConnectionPolicy.
     */
    public void transmit(Envelope envelope) {
        envelopeRegistry.register(envelope);
        sessions.entrySet().stream()
                // routing
                .filter(e -> connectionPolicy.routePacketThrough(getSelf(), e.getKey(), envelope.getRecipients()))
                        // don't send back to source if this packet was received
                .filter(e -> !envelope.getSender().isPresent() || !envelope.getSender().get().equals(e.getKey()))
                        // filter active sessions
                .map(Map.Entry::getValue).map(o -> o.orElse(null)).filter(s -> s != null)
                // transmit
                .forEach(s -> {
                    ByteBuf data = Unpooled.buffer();
                    data.writeLong(envelope.getId());
                    recipientFactory.writeRange(data, envelope.getRecipients());
                    data.writeBytes(envelope.getPayload());
                    s.transmit(data);
                });
    }
}
