package at.yawk.accordion.compression;

import io.netty.buffer.ByteBuf;
import lombok.Getter;

/**
 * No-operation Compressor implementation, performs no compression.
 *
 * @author yawkat
 */
public class VoidCompressor implements Compressor {
    @Getter private static final Compressor instance = new VoidCompressor();

    private VoidCompressor() {}

    @Override
    public ByteBuf encode(ByteBuf message) {
        return message;
    }

    @Override
    public ByteBuf decode(ByteBuf encoded) {
        return encoded;
    }
}
