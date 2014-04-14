package at.yawk.accordion.p2p;

import io.netty.buffer.ByteBuf;

/**
 * Serialization and deserialization for application-specific Recipients.
 *
 * @author Yawkat
 */
public interface RecipientFactory {
    /**
     * Deserialize a RecipientRange.
     */
    RecipientRange readRange(ByteBuf source);

    /**
     * Deserialize a Recipient.
     *
     * @param useCache If set to true, this method should use a global cache to minimize bandwidth usage, at risk of
     *                 returning null.
     */
    Recipient read(ByteBuf source, boolean useCache);

    /**
     * Serialize a RecipientRange. Can be assumed to be the same type as returned by #readRange.
     */
    void writeRange(ByteBuf target, RecipientRange recipient);

    /**
     * Serialize a Recipient. Can be assumed to be the same type as returned by #read.
     *
     * @param useCache If set to true, this method should use a global cache to minimize bandwidth usage.
     */
    void write(ByteBuf target, Recipient recipient, boolean useCache);
}
