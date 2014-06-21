package at.yawk.accordion.minecraft;

import at.yawk.accordion.distributed.ConnectionListener;
import at.yawk.accordion.distributed.LocalAddressSorter;
import at.yawk.accordion.distributed.LocalNode;
import at.yawk.accordion.distributed.Node;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Default connection listener implementation. Uses bungee servers as "backbone" servers for the bukkit servers to
 * connect to.
 *
 * @author yawkat
 */
public class DefaultMinecraftConnectionListener implements ConnectionListener {
    /**
     * Time in milliseconds after an unsuccesful connection attempt that we should avoid using a node to connect to.
     */
    private static final int DISCONNECT_AVOID_INTERVAL = 10000;

    private final LocalNode localNode;

    private final TObjectLongMap<Node> nodeFailTimes = new TObjectLongHashMap<>();

    private final Comparator<Node> nodeSorter;

    protected DefaultMinecraftConnectionListener(LocalNode localNode) {
        this.localNode = localNode;

        nodeSorter = Comparator.
                // avoid nodes we polled unsuccessfully in the last 10 seconds
                        <Node, Boolean>comparing(node -> System.currentTimeMillis() - nodeFailTimes.get(node) >
                                                         DISCONNECT_AVOID_INTERVAL)
                        // prefer nodes that are closer to our address
                .thenComparing(Comparator.comparing(node -> node.getAddress().getAddress(),
                                                    new LocalAddressSorter(localNode.getSelf()
                                                                                   .getAddress()
                                                                                   .getAddress())));
    }

    public static ConnectionListener create(LocalNode localNode) {
        return new DefaultMinecraftConnectionListener(localNode);
    }

    /**
     * Return whether the given node is a control / backbone node.
     */
    protected boolean isControlNode(Node node) {
        return node.getTier() == AccordionApi.DEFAULT_TIER_BUNGEE;
    }

    /**
     * Return whether we are a control / backbone node.
     */
    private boolean isControlNode() {
        return isControlNode(localNode.getSelf());
    }

    @Override
    public void nodesRegistered(Set<Node> newNodes, boolean fromSynchronization) {
        update();
    }

    @Override
    public void connectionAttemptFailed(Node other) {
        // connection failed, wait a bit until retry
        nodeFailTimes.put(other, System.currentTimeMillis());
        update();
    }

    @Override
    public void connected(Node other, boolean thisIsServer) {
        update();
    }

    @Override
    public void disconnected(Node other) {
        update();
    }

    /**
     * Whether we should try connecting to more remote nodes.
     */
    private boolean needsMoreNodes() {
        return isControlNode() || localNode.getConnectedNodes().isEmpty();
    }

    /**
     * Called when anything is updated.
     */
    private synchronized void update() {
        if (needsMoreNodes()) {
            // try to connect to a new node
            Stream<Node> possibleNodes = localNode.getKnownNodes().stream()
                    // don't connect to us
                    .filter(node -> !node.equals(localNode.getSelf()))
                            // already connected to that
                    .filter(node -> !localNode.getConnectedNodes().contains(node))
                            // connect to control only
                    .filter(this::isControlNode)
                            // sort by IP and failed connection time
                    .sorted(nodeSorter);
            // find best node
            Optional<Node> chosen = possibleNodes.findFirst();
            chosen.ifPresent(localNode::connect);
        }
    }
}
