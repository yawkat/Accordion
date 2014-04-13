package at.yawk.accordion.bare;

import com.google.common.collect.Lists;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;
import lombok.extern.log4j.Log4j;

/**
 * Raw network manager. Handles connecting to servers and binding a server.
 *
 * @author Yawkat
 */
@Log4j
public class NetworkManager {
    /**
     * Attempt to connect to the server at the given address. Blocking.
     *
     * @return The created Session if we connected successfully, null otherwise.
     */
    @Nullable
    public Session connect(SocketAddress remote) {
        // initialize

        // TODO find a non-hacky method to close channels
        final Collection<Channel> registeredChannels = Collections.synchronizedList(Lists.<Channel>newArrayList());

        EventLoopGroup workerGroup = new NioEventLoopGroup() {
            @Override
            public ChannelFuture register(Channel channel, ChannelPromise promise) {
                registeredChannels.add(channel);
                return super.register(channel, promise);
            }
        };

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                 .handler(new ChannelHandlerAdapter() {})
                 .channel(NioSocketChannel.class)
                 .option(ChannelOption.SO_KEEPALIVE, true);
        try {
            // connect
            ChannelFuture f = bootstrap.connect(remote);
            f.sync();
            Session session = new Session(this, f.channel(), Direction.TO_SERVER);
            session.prepareChannel();
            return session;
        } catch (Exception e) {
            // failed, attempt to close any open connections
            workerGroup.shutdownGracefully();
            registeredChannels.forEach(Session::closeChannel);
            return null;
        }
    }

    /**
     * Create a server that can be bound to listen on a port.
     */
    public Server createServer() {
        Server server = new Server(this);
        server.prepare();
        return server;
    }
}
