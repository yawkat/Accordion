/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.netty;

import at.yawk.accordion.codec.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Raw connection to another Node.
 *
 * @author yawkat
 */
public interface Connection {
    /**
     * Send the given ByteBuf through this connection. The ByteBuf should not be used again.
     */
    void send(ByteBuf data);

    /**
     * Disconnect from the remote.
     */
    void disconnect();

    /**
     * Set the message handler that will be called when a message arrives through this connection.
     */
    void setMessageHandler(Consumer<ByteBuf> listener);

    /**
     * Set the exception handler that will be called when an error occurs in this connection.
     */
    void setExceptionHandler(Consumer<Throwable> listener);

    /**
     * Set the listener that should be invoked when this connection is disconnected.
     */
    void setDisconnectHandler(Runnable listener);

    /**
     * Get a mutable, thread-safe map where applications can store connection-specific data.
     */
    Map<String, Object> properties();
}
