package at.yawk.accordion.packet;

import io.netty.buffer.ByteBuf;

/**
 * Utilities for reading and writing special values to ByteBufs.
 *
 * @author Yawkat
 */
public class StreamUtil {
    private StreamUtil() {}

    /**
     * Write a google protobuf varint to the given stream.
     */
    public static void writeVarInt(ByteBuf target, int payload) {
        // max 5 bytes, handle first 4 here
        for (int i = 4; i > 0; i--) {
            // check if we need to write this septet (if this or any previous ones contain data)
            int sept = payload >>> (7 * i);
            if (sept != 0) {
                target.writeByte(0x7F | (sept & 0x7F));
            }
        }
        // write last byte, mask 01111111
        target.writeByte(payload & 0x7F);
    }

    /**
     * Read a google protobuf varint from the given stream.
     */
    public static int readVarInt(ByteBuf source) {
        int result = 0;
        while (true) {
            byte sept = source.readByte();
            result = (result << 7) | (sept & 0x7FF);
            if ((sept & 0x80) == 0) { break; }
        }
        return result;
    }
}
