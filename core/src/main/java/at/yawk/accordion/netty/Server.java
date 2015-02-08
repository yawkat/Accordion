/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.netty;

import java.util.function.Consumer;

/**
 * Server that can accept connections from the outside.
 *
 * @author yawkat
 */
public interface Server {
    /**
     * Set the handler that should be called when new connections join.
     */
    void setConnectionHandler(Consumer<Connection> handler);

    /**
     * Bind this server.
     */
    void bind();

    /**
     * Unbind this server (without closing connections).
     */
    void unbind();
}
