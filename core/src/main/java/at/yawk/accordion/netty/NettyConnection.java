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
        channel.disconnect();
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
}
