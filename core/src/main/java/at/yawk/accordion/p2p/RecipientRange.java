package at.yawk.accordion.p2p;

/**
 * A range of Recipients. May include a single recipient (Computer xyz), a special group of recipients (All databases)
 * or all nodes.
 *
 * @author Yawkat
 */
public interface RecipientRange {
    /**
     * Whether this range contains the given recipient.
     */
    boolean contains(Recipient recipient);

    /**
     * Whether this range contains all recipients that are also included in the given range (if the given range is a
     * subset of this one). The given range can be assumed to be of the same type as this range.
     */
    boolean containsAll(RecipientRange range);
}
