/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;

/**
 * @author yawkat
 */
abstract class PrimitiveCodec implements UnsafeCodec {
    public static final PrimitiveCodec BOOLEAN = new PrimitiveCodec() {
        @Override
        public void read(ByteBuf from, Object to, long offset) {
            UnsafeAccess.unsafe.putBoolean(to, offset, from.readBoolean());
        }

        @Override
        public void write(ByteBuf to, Object from, long offset) {
            to.writeBoolean(UnsafeAccess.unsafe.getBoolean(from, offset));
        }
    };
    public static final PrimitiveCodec BYTE = new PrimitiveCodec() {
        @Override
        public void read(ByteBuf from, Object to, long offset) {
            UnsafeAccess.unsafe.putByte(to, offset, from.readByte());
        }

        @Override
        public void write(ByteBuf to, Object from, long offset) {
            to.writeByte(UnsafeAccess.unsafe.getByte(from, offset));
        }
    };
    public static final PrimitiveCodec SHORT = new PrimitiveCodec() {
        @Override
        public void read(ByteBuf from, Object to, long offset) {
            UnsafeAccess.unsafe.putShort(to, offset, from.readShort());
        }

        @Override
        public void write(ByteBuf to, Object from, long offset) {
            to.writeShort(UnsafeAccess.unsafe.getShort(from, offset));
        }
    };
    public static final PrimitiveCodec CHAR = new PrimitiveCodec() {
        @Override
        public void read(ByteBuf from, Object to, long offset) {
            UnsafeAccess.unsafe.putChar(to, offset, from.readChar());
        }

        @Override
        public void write(ByteBuf to, Object from, long offset) {
            to.writeChar(UnsafeAccess.unsafe.getChar(from, offset));
        }
    };
    public static final PrimitiveCodec INT = new PrimitiveCodec() {
        @Override
        public void read(ByteBuf from, Object to, long offset) {
            UnsafeAccess.unsafe.putInt(to, offset, from.readInt());
        }

        @Override
        public void write(ByteBuf to, Object from, long offset) {
            to.writeInt(UnsafeAccess.unsafe.getInt(from, offset));
        }
    };
    public static final PrimitiveCodec LONG = new PrimitiveCodec() {
        @Override
        public void read(ByteBuf from, Object to, long offset) {
            UnsafeAccess.unsafe.putLong(to, offset, from.readLong());
        }

        @Override
        public void write(ByteBuf to, Object from, long offset) {
            to.writeLong(UnsafeAccess.unsafe.getLong(from, offset));
        }
    };
    public static final PrimitiveCodec FLOAT = new PrimitiveCodec() {
        @Override
        public void read(ByteBuf from, Object to, long offset) {
            UnsafeAccess.unsafe.putFloat(to, offset, from.readFloat());
        }

        @Override
        public void write(ByteBuf to, Object from, long offset) {
            to.writeFloat(UnsafeAccess.unsafe.getFloat(from, offset));
        }
    };
    public static final PrimitiveCodec DOUBLE = new PrimitiveCodec() {
        @Override
        public void read(ByteBuf from, Object to, long offset) {
            UnsafeAccess.unsafe.putDouble(to, offset, from.readDouble());
        }

        @Override
        public void write(ByteBuf to, Object from, long offset) {
            to.writeDouble(UnsafeAccess.unsafe.getDouble(from, offset));
        }
    };

    @Override
    public ByteCodec<?> toByteCodec() {
        throw new UnsupportedOperationException("Primitive codec");
    }
}
