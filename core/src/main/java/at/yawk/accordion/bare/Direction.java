package at.yawk.accordion.bare;

/**
 * Direction in which a session was initiated.
 *
 * @author Jonas Konrad (yawkat)
 */
public enum Direction {
    /**
     * The session was created when another peer connected to our listener as a client.
     */
    TO_CLIENT,
    /**
     * The session was created when we actively connected to the listener of another peer.
     */
    TO_SERVER,
}
