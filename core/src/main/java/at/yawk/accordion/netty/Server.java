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
