/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.distributed;

import at.yawk.accordion.Log;
import at.yawk.accordion.netty.Connection;
import io.netty.buffer.Unpooled;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Detects silent disconnects by sending heartbeats and watching for timeouts.
 *
 * @author yawkat
 */
class HeartbeatManager {
    /**
     * Interval in milliseconds between sent heartbeats.
     */
    private static final int SEND_INTERVAL = 3000;
    /**
     * Maximum timeout between heartbeats. If a server doesn't heartbeat within this interval it is disconnected.
     * <p/>
     * Larger than SEND_INTERVAL to compensate for connection lag.
     */
    private static final int TIMEOUT = SEND_INTERVAL * 2;

    private static final AtomicInteger threadId = new AtomicInteger();

    /**
     * Our ConnectionManager.
     */
    private final ConnectionManager connectionManager;

    private final ScheduledExecutorService heartbeat;

    /**
     * Flag whether #start has been called.
     */
    private boolean scheduled = false;

    public HeartbeatManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;

        heartbeat = Executors
                .newSingleThreadScheduledExecutor(r -> new Thread(connectionManager.getThreadGroup(),
                                                                  r,
                                                                  "Heartbeat thread #" + threadId.incrementAndGet()));

        connectionManager.setInternalHandler(InternalProtocol.HEARTBEAT, (msg, con) -> {});
    }

    /**
     * Start sending heartbeats to connected nodes.
     */
    public synchronized void start() {
        if (scheduled) {
            throw new IllegalStateException("Heartbeat already scheduled");
        }
        scheduled = true;

        heartbeat.scheduleAtFixedRate(this::beat, 0, SEND_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Send a heartbeat to all connected servers and check if they are still online.
     */
    private void beat() {
        connectionManager.getConnections().forEach(connection -> {
            // send heartbeat
            try {
                connectionManager
                        .sendPacket(InternalProtocol.HEARTBEAT_BYTES, Stream.of(connection), Unpooled.EMPTY_BUFFER);
            } catch (Throwable t) {
                connectionManager.getLogger().error("Failed to send heartbeat to " + connection, t);
            }
            // check for timeout
            try {
                Object lastHeartbeatObj = connection.properties().get("lastHeartbeat");
                // null if no hb packet was received yet and onConnected wasn't called for some reason.
                if (lastHeartbeatObj != null) {
                    long timeSinceLastHeartbeat = System.currentTimeMillis() - (long) lastHeartbeatObj;
                    if (timeSinceLastHeartbeat > TIMEOUT) {
                        // timeout
                        Log.info(connectionManager.getLogger(), () -> connection + " timed out (heartbeat)");
                        connection.disconnect();
                    }
                }
            } catch (Throwable t) {
                connectionManager.getLogger().error("Failed to confirm heartbeat for " + connection, t);
            }
        });
    }

    public void onConnected(Connection connection) {
        markAlive(connection);
    }

    /**
     * Mark a connection as alive (set the "last heartbeat" attribute to the current time).
     */
    public void markAlive(Connection connection) {
        connection.properties().put("lastHeartbeat", System.currentTimeMillis());
    }
}
