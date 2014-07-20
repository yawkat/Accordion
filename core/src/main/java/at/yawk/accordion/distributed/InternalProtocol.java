package at.yawk.accordion.distributed;

import at.yawk.accordion.compression.Compressor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;

/**
 * Util class for some internal protocol stuff.
 *
 * @author yawkat
 */
class InternalProtocol {
    /**
     * Channel used for subscribtion management.
     */
    static final String SUBSCRIBE = "acc.sub";
    /**
     * Channel used for node management.
     */
    static final String SYNC_NODES = "acc.nod";
    /**
     * Channel used for handshake packet.
     */
    static final String WELCOME = "acc.hi";
    /**
     * Encoded #WELCOME.
     */
    static final byte[] WELCOME_BYTES = WELCOME.getBytes(StandardCharsets.UTF_8);
    /**
     * Channel used for heartbeat handler.
     */
    static final String HEARTBEAT = "acc.bea";
    /**
     * Encoded #HEARTBEAT.
     */
    static final byte[] HEARTBEAT_BYTES = HEARTBEAT.getBytes(StandardCharsets.UTF_8);

    /**
     * Read a string written with #writeByteString.
     */
    static String readByteString(ByteBuf message) {
        return new String(readByteArray(message), StandardCharsets.UTF_8);
    }

    /**
     * Read a byte array written by #writeByteArray.
     */
    static byte[] readByteArray(ByteBuf from) {
        byte[] array = new byte[from.readUnsignedByte()];
        from.readBytes(array);
        return array;
    }

    /**
     * Write a string. Maximum encoded length 0xff bytes.
     */
    static void writeByteString(ByteBuf to, String string) {
        writeByteArray(to, string.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Write a byte array. Maximum length 0xff bytes.
     */
    static void writeByteArray(ByteBuf to, byte[] array) {
        to.writeByte(array.length);
        to.writeBytes(array);
    }

    /**
     * Encode a packet to be read by other nodes.
     */
    static ByteBuf encodePacket(byte[] typeBytes, long id, ByteBuf payload, Compressor compressor) {
        ByteBuf body = Unpooled.buffer();
        // channel
        writeByteArray(body, typeBytes);
        // payload
        body.writeBytes(payload);

        ByteBuf compressedBody = compressor.encode(body);

        ByteBuf full = Unpooled.buffer();
        // id header
        full.writeLong(id);
        // body
        full.writeBytes(compressedBody);
        return full;
    }
}
