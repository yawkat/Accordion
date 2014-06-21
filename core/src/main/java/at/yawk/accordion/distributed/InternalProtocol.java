package at.yawk.accordion.distributed;

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
    static ByteBuf encodePacket(byte[] typeBytes, long id, ByteBuf payload) {
        ByteBuf full = Unpooled.buffer();
        // headers
        full.writeLong(id);
        writeByteArray(full, typeBytes);
        // payload
        full.writeBytes(payload);
        return full;
    }
}
