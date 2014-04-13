package at.yawk.accordion.bare;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Split a byte stream into chunks.
 *
 * @see at.yawk.accordion.bare.Framer
 *
 * @author Jonas Konrad (yawkat)
 */
@RequiredArgsConstructor
class Splitter extends ByteToMessageDecoder {
    private final Session session;

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext,
                          @NonNull ByteBuf byteBuf,
                          @NonNull List<Object> objects) {
        if (byteBuf.readableBytes() < 2) { return; }

        byteBuf.markReaderIndex();
        int len = byteBuf.readUnsignedShort();

        if (byteBuf.readableBytes() < len) {
            byteBuf.resetReaderIndex();
            return;
        }

        objects.add(byteBuf.readBytes(len));
    }
}
