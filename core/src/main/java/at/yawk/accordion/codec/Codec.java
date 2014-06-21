package at.yawk.accordion.codec;

/**
 * Bidirectional converter from an encoded type to a decoded type.
 *
 * @author yawkat
 */
public interface Codec<T, U> {
    /**
     * Encode the given object.
     */
    T encode(U message);

    /**
     * Decode the given object.
     */
    U decode(T encoded);
}
