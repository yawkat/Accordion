package at.yawk.accordion.bare;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j;

/**
 * Bindable server.
 *
 * @author Yawkat
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Setter
@Log4j
public class Server {
    /**
     * The owning NetworkManager.
     */
    @NonNull private final NetworkManager networkManager;
    /**
     * The ServerBootstrap singleton.
     */
    @NonNull private final ServerBootstrap serverBootstrap = new ServerBootstrap();

    /**
     * The session handler: called whenever a client connects to this Server.
     *
     * A setter exists in #setSessionHandler.
     */
    @NonNull private Consumer<Session> sessionHandler = s -> {};

    /**
     * Prepare this Server (does not bind!). Must only be called once.
     */
    void prepare() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        serverBootstrap.group(bossGroup, workerGroup)
                       .channel(NioServerSocketChannel.class)
                       .childHandler(new ServerInitializer())
                       .option(ChannelOption.SO_BACKLOG, 128)
                       .childOption(ChannelOption.SO_KEEPALIVE, true);
    }

    /**
     * Bind to the given port at 0.0.0.0.
     *
     * @see #bind(java.net.SocketAddress)
     */
    public BoundServer bind(int port) {
        return bind(new InetSocketAddress("0.0.0.0", port));
    }

    /**
     * Bind to the given address.
     *
     * @return A bound server handle.
     */
    public BoundServer bind(SocketAddress address) {
        log.info("Accordion listening on " + address);
        ChannelFuture future = serverBootstrap.bind(address);
        return new BoundServer(this, future);
    }

    /**
     * Closeable server handle. Closing it will unbind the server.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BoundServer implements Closeable {
        private final Server owner;
        private final ChannelFuture channelFuture;

        @Override
        public void close() throws IOException {
            log.info("Accordion stopped listening.");
            channelFuture.channel().close();
        }
    }

    /**
     * Raw connection listener.
     */
    @RequiredArgsConstructor
    private class ServerInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(@NonNull SocketChannel ch) {
            Session session = new Session(networkManager, ch, Direction.TO_CLIENT);
            session.prepareChannel();
            sessionHandler.accept(session);
        }
    }
}
