package at.yawk.accordion.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import lombok.Getter;
import org.xerial.snappy.Snappy;

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
        try {
            return Unpooled.wrappedBuffer(Snappy.compress(toArray(raw)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ByteBuf decode(ByteBuf compressed) {
        try {
            return Unpooled.wrappedBuffer(Snappy.uncompress(toArray(compressed)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] toArray(ByteBuf raw) {
        return Arrays.copyOfRange(raw.array(),
                                  raw.arrayOffset() + raw.readerIndex(),
                                  raw.arrayOffset() + raw.readerIndex() + raw.readableBytes());
    }
}
