/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import java.net.SocketAddress;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Netty implementation of Server.
 *
 * @author yawkat
 */
@RequiredArgsConstructor
class NettyServer implements Server {
    private final ServerBootstrap bootstrap;
    private final SocketAddress inf;

    private ChannelFuture future;

    @Setter private Consumer<Connection> connectionHandler = connection -> {};

    /**
     * Initialize this server so connections can be accepted (does not bind!).
     */
    void init() {
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                NettyConnection connection = new NettyConnection(ch);
                connection.init();
                connectionHandler.accept(connection);
            }
        });
    }

    @Override
    public void bind() {
        future = bootstrap.bind(inf);
    }

    @Override
    public void unbind() {
        future.channel().close();
    }
}
