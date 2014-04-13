package at.yawk.accordion.bare;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;

/**
 * Prefixes each packet with a length short so the Splitter can split them up on the other side.
 *
 * @author Jonas Konrad (yawkat)
 */
@RequiredArgsConstructor
@Log4j
class Framer extends MessageToByteEncoder<ByteBuf> {
    private final Session session;

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, @NonNull ByteBuf src, @NonNull ByteBuf out) {
        int len = src.readableBytes();
        if (len > 20000) { log.warn("Very long message (" + len + " bytes!)"); }
        out.writeShort(len);
        out.writeBytes(src);
    }
}
