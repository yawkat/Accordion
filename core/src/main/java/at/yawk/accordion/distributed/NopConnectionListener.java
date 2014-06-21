package at.yawk.accordion.distributed;

import lombok.Getter;

/**
 * ConnectionListener implementation that does nothing.
 *
 * @author yawkat
 */
public class NopConnectionListener implements ConnectionListener {
    /**
     * The singleton instance.
     */
    @Getter private static final ConnectionListener instance = new NopConnectionListener();

    /**
     * getInstance alias that can be used as a ConnectionListenerFactory via method reference.
     */
    @SuppressWarnings("UnusedParameters")
    public static ConnectionListener getInstance(LocalNode localNode) {
        return getInstance();
    }

    private NopConnectionListener() {}
}
