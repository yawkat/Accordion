package at.yawk.accordion.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;

/**
 * Netty Connector implementation.
 *
 * @author yawkat
 */
public class NettyConnector implements Connector {
    /**
     * Value used for SO_BACKLOG server socket property. Defines how many connection requests may be opened before new
     * ones are refused.
     */
    private static final int BACKLOG_VALUE = 128;

    /**
     * Singleton instance.
     */
    @Getter private static final Connector instance = new NettyConnector();

    private NettyConnector() {}

    @Override
    public Optional<Connection> connect(SocketAddress address) {
        // TODO find a non-hacky method to close channels
        Collection<Channel> registeredChannels = Collections.synchronizedList(new ArrayList<>());

        EventLoopGroup workerGroup = new NioEventLoopGroup() {
            @Override
            public ChannelFuture register(Channel channel, ChannelPromise promise) {
                registeredChannels.add(channel);
                return super.register(channel, promise);
            }
        };

        AtomicReference<Connection> connectionRef = new AtomicReference<>();

        // init
        Bootstrap bootstrap = new Bootstrap()
                .group(workerGroup)
                .handler(new ChannelHandlerAdapter() {
                    @Override
                    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
                        NettyConnection connection = new NettyConnection(ctx.channel());
                        connectionRef.set(connection);
                        connection.init();
                    }
                })
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true);

        try {
            // connect
            ChannelFuture connectFuture = bootstrap.connect(address);
            // wait for connection
            connectFuture.sync();
            return Optional.of(connectionRef.get());
        } catch (Exception e) {
            // shut down workers
            workerGroup.shutdownGracefully();

            // kill channels
            registeredChannels.forEach(NettyConnection::close);

            return Optional.empty();
        }
    }

    @Override
    public Server listen(SocketAddress inf) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();

        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, BACKLOG_VALUE)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        NettyServer server = new NettyServer(bootstrap, inf);
        server.init();

        return server;
    }
}
