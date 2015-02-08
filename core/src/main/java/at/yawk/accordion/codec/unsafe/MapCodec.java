/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * @author yawkat
 */
class MapCodec<M extends Map<?, ?>> extends GenericCodecSupplier<M> {
    private final IntFunction<? extends M> factory;

    public MapCodec(Class<M> type, IntFunction<? extends M> factory) {
        super(type);
        this.factory = factory;
    }

    @Override
    protected ByteCodec<M> createCodec(CodecSupplier registry, FieldWrapper field) {
        FieldWrapper keyType = field.genericTypeOrThrow(0);
        ByteCodec keyCodec = registry.getCodecOrThrow(keyType)
                .toByteCodec();
        FieldWrapper valueType = field.genericTypeOrThrow(1);
        ByteCodec valueCodec = registry.getCodecOrThrow(valueType)
                .toByteCodec();

        return new ByteCodec<M>() {
            @Override
            public void encode(ByteBuf target, M message) {
                target.writeInt(message.size());
                for (Map.Entry o : message.entrySet()) {
                    keyCodec.encode(target, o.getKey());
                    valueCodec.encode(target, o.getValue());
                }
            }

            @Override
            public M decode(ByteBuf encoded) {
                int size = encoded.readInt();
                Map map = factory.apply(size);
                for (int i = 0; i < size; i++) {
                    Object k = keyCodec.decode(encoded);
                    Object v = valueCodec.decode(encoded);
                    map.put(k, v);
                }
                return (M) map;
            }
        };
    }
}
