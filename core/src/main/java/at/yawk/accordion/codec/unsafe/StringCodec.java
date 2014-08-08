package at.yawk.accordion.codec.unsafe;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/**
 * @author yawkat
 */
class StringCodec extends UnsafeCodec<String> {
    @Override
    public void encode(ByteBuf target, String message) {
        target.writeShort(message.length());
        target.writeBytes(message.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decode(ByteBuf encoded) {
        byte[] bytes = new byte[encoded.readUnsignedShort()];
        encoded.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
