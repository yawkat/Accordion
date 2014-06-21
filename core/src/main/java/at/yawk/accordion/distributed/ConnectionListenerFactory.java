package at.yawk.accordion.distributed;

/**
 * Functional interface to create a ConnectionListener for a LocalNode.
 *
 * @author yawkat
 */
@FunctionalInterface
public interface ConnectionListenerFactory {
    /**
     * Create a new ConnectionListener for the specific LocalNode.
     */
    ConnectionListener createConnectionListener(LocalNode localNode);
}
