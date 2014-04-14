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
    public static void writeVarInt(ByteBuf target, long payload) {
        // max 9 bytes, handle first 8 here
        for (int i = 9; i > 0; i--) {
            // check if we need to write this septet (if this or any previous ones contain data)
            long sept = payload >>> (7 * i);
            if (sept != 0) {
                target.writeByte((int) (0x80 | (sept & 0x7F)));
            }
        }
        // write last byte, mask 01111111
        target.writeByte((int) (payload & 0x7F));
    }

    public static int readVarInt(ByteBuf source) {
        return (int) readVarIntLong(source);
    }

    /**
     * Read a google protobuf varint from the given stream.
     */
    public static long readVarIntLong(ByteBuf source) {
        long result = 0;
        while (true) {
            byte sept = source.readByte();
            result = (result << 7) | (sept & 0x7F);
            if ((sept & 0x80) == 0) { break; }
        }
        return result;
    }

    /**
     * Reads a byte array of variable length.
     */
    public static byte[] readDynamicByteArray(ByteBuf source) {
        byte[] result = new byte[readVarInt(source)];
        source.readBytes(result);
        return result;
    }

    /**
     * Writes a byte array of variable length.
     */
    public static void writeDynamicByteArray(ByteBuf target, byte[] data) {
        writeVarInt(target, data.length);
        target.writeBytes(data);
    }
}
