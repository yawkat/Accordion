/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @author yawkat
 */
class NamedObjectCodec implements ByteCodec<Object> {
    private final CodecSupplier directCodecs;

    public NamedObjectCodec(CodecSupplier codecs) {
        directCodecs = new CachedCodecSupplier((m, f) -> CommonObjectCodec.factory(codecs, f));
    }

    public Optional<UnsafeCodec> factory(CodecSupplier registry, FieldWrapper field) {
        return Optional.of(new UnsafeByteCodec<>(this));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void encode(ByteBuf target, Object message) {
        if (message == null) {
            target.writeByte(0);
        } else {
            Class<?> clazz = message.getClass();
            writeClassName(target, clazz.getName());
            getCodec(clazz).encode(target, message);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object decode(ByteBuf encoded) {
        String className = readClassName(encoded);
        if (className.isEmpty()) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName(className);
            return getCodec(clazz).decode(encoded);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ByteCodec getCodec(Class<?> clazz) {
        return (ByteCodec) directCodecs.getCodecOrThrow(FieldWrapper.clazz(clazz)).toByteCodec();
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
