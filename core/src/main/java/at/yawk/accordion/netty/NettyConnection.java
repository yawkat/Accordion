/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Netty implementation of the Connection class.
 *
 * @author yawkat
 */
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
class NettyConnection implements Connection {
    private final Channel channel;

    private final List<ByteBuf> messageQueue = new ArrayList<>();

    private Consumer<ByteBuf> messageHandler = message -> {
        synchronized (messageQueue) {
            messageQueue.add(message);
        }
    };
    @Setter private Consumer<Throwable> exceptionHandler = opt -> {};
    @Setter private Runnable disconnectHandler = () -> {};

    private final Map<String, Object> properties = new ConcurrentHashMap<>();

    /**
     * Initialize this channel so messages can be read.
     */
    void init() {
        channel.pipeline()
                .addLast(new Framer())
                .addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        messageHandler.accept((ByteBuf) msg);
                    }
                })
                .addLast(new ChannelDuplexHandler() {
                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        exceptionHandler.accept(cause);
                    }

                    @Override
                    public void disconnect(ChannelHandlerContext ctx, ChannelPromise future) throws Exception {
                        disconnectHandler.run();
                    }
                });
    }

    @Override
    public void send(ByteBuf data) {
        channel.writeAndFlush(data);
    }

    @Override
    public void disconnect() {
        close(channel);
        disconnectHandler.run();
    }

    @Override
    public void setMessageHandler(Consumer<ByteBuf> listener) {
        this.messageHandler = listener;
        synchronized (messageQueue) {
            messageQueue.stream().forEach(listener::accept);
            messageQueue.clear();
        }
    }

    @Override
    public Map<String, Object> properties() {
        return properties;
    }

    /**
     * Tries to close a channel through a bunch of different methods, ensuring no file descriptors are leaked.
     */
    static void close(Channel channel) {
        try {
            // first, disconnect
            channel.disconnect().addListener(f -> {
                try {
                    // close
                    channel.close().addListener(g -> {
                        try {
                            // use unsafe access to make sure underlying socket is closed
                            channel.unsafe().closeForcibly();
                        } catch (Exception ignored) {}
                    });
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }
}
