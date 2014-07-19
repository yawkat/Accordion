package at.yawk.accordion.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.compression.Snappy;
import lombok.Getter;

/**
 * Compressor implementation that uses snappy to compress data.
 *
 * @author yawkat
 */
public class SnappyCompressor implements Compressor {
    @Getter private static final Compressor instance = new SnappyCompressor();

    private SnappyCompressor() {}

    @Override
    public ByteBuf encode(ByteBuf raw) {
        // fairly cheap
        Snappy snappy = new Snappy();

        ByteBuf output = Unpooled.buffer();
        snappy.encode(raw, output, raw.readableBytes());
        return output;
    }

    @Override
    public ByteBuf decode(ByteBuf compressed) {
        // fairly cheap
        Snappy snappy = new Snappy();

        ByteBuf output = Unpooled.buffer();
        snappy.decode(compressed, output);
        return output;
    }
}
