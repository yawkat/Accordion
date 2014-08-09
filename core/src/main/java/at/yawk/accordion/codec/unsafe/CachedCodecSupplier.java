package at.yawk.accordion.codec.unsafe;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class CachedCodecSupplier implements CodecSupplier {
    private final CodecSupplier delegate;
    private final Map<FieldWrapper, Optional<UnsafeCodec>> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<UnsafeCodec> getCodec(CodecSupplier registry, FieldWrapper field) {
        return cache.computeIfAbsent(field, f -> delegate.getCodec(this, f));
    }
}
