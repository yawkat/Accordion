package at.yawk.accordion.bare;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.function.Consumer;
import lombok.*;
import lombok.extern.log4j.Log4j;

/**
 * A Session is the wrapper class for a single connection to another node.
 *
 * @author Yawkat
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Setter
@Log4j
public class Session {
    @NonNull @Getter private final NetworkManager networkManager;
    /**
     * The netty channel.
     */
    @NonNull private final Channel channel;
    /**
     * The direction in which this Session was created.
     */
    @NonNull @Getter private final Direction direction;

    /**
     * The message handler: Called when a message is received from the other end. Default operation is nothing.
     */
    @NonNull private Consumer<ByteBuf> messageHandler = b -> {};
    /**
     * The error handler: Called when an error happens. Default operation is logging to console and disconnecting.
     */
    @NonNull private Consumer<Throwable> errorHandler = t -> {
        log.error(t);
        disconnect();
    };
    /**
     * The destruction handler: Called when #destroy is invoked.
     */
    @NonNull private Runnable destructionListener = () -> {};

    /**
     * Send a message to the other end.
     */
    public void transmit(ByteBuf message) {
        channel.eventLoop().execute(() -> channel.writeAndFlush(message));
    }

    /**
     * Prepare this channel. Must only be called once.
     */
    void prepareChannel() {
        // decoding
        channel.pipeline().addLast(new Framer(this)).addLast(new Splitter(this));

        // handling
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(@NonNull ChannelHandlerContext ctx, Object msg) {
                assert msg instanceof ByteBuf;
                messageHandler.accept((ByteBuf) msg);
            }
        });

        // error handling
        channel.pipeline().addFirst(new ChannelDuplexHandler() {
            @Override
            public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
                destroy();
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                errorHandler.accept(cause);
            }
        });
    }

    /**
     * Disconnect from the remote end.
     */
    public void disconnect() {
        if (isConnected()) {
            channel.disconnect();
            destroy();
        }
    }

    /**
     * Destroy this session. This usually removes the session from any place it might be registered at. Should only be
     * called once. Does not disconnect the channel.
     */
    public void destroy() {
        destructionListener.run();
    }

    /**
     * @return whether this session is connected.
     */
    public boolean isConnected() {
        return channel.isOpen();
    }

    /**
     * Forcibly close the given channel.
     */
    static void closeChannel(@NonNull Channel channel) {
        channel.close();
        channel.unsafe().closeForcibly();
    }
}
