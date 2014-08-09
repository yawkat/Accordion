package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;

/**
 * @author yawkat
 */
public class CodecManagerBuilder {
    private final CodecManager manager = new CodecManager();

    private CodecManagerBuilder() {}

    public static CodecManagerBuilder create() {
        return new CodecManagerBuilder();
    }

    public <T> CodecManagerBuilder addObjectCodec(Class<T> clazz, ByteCodec<T> codec) {
        manager.addObjectCodec(clazz, codec);
        return this;
    }

    public CodecManager build() {
        return manager.copy();
    }
}
