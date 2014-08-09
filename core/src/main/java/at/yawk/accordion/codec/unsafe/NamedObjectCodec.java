package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class NamedObjectCodec implements ByteCodec<Object> {
    private final CodecSupplier codecs;

    public static Optional<UnsafeCodec> factory(CodecSupplier registry, FieldWrapper field) {
        return Optional.of(new UnsafeByteCodec<>(new NamedObjectCodec(registry)));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void encode(ByteBuf target, Object message) {
        if (message == null) {
            target.writeByte(0);
        } else {
            Class<?> clazz = message.getClass();
            writeClassName(target, clazz.getName());
            ((ByteCodec) codecs.getCodecOrThrow(FieldWrapper.clazz(clazz)).toByteCodec()).encode(target, message);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object decode(ByteBuf encoded) {
        String className = readClassName(encoded);
        if (className.isEmpty()) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName(className);
            return ((ByteCodec) codecs.getCodecOrThrow(FieldWrapper.clazz(clazz)).toByteCodec()).decode(encoded);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static String readClassName(ByteBuf buf) {
        int len = buf.bytesBefore((byte) 0);
        byte[] data = new byte[len];
        buf.readBytes(data);
        buf.skipBytes(1); // \0
        return new String(data, StandardCharsets.UTF_8);
    }

    private static void writeClassName(ByteBuf buf, String className) {
        buf.writeBytes(className.getBytes(StandardCharsets.UTF_8));
        buf.writeByte(0);
    }
}
