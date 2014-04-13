package at.yawk.accordion.minecraft;

import at.yawk.accordion.p2p.Recipient;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A single server ("node"). Uniquely identified by category and ID, meaning that no two servers should share both the
 * same category and id.
 *
 * @author Yawkat
 */
@RequiredArgsConstructor(staticName = "create")
@EqualsAndHashCode(of = {"category", "id"})
public class Server implements Recipient {
    /**
     * The main category of this server ("databases", "hub servers", etc).
     */
    @Getter private final ServerCategory category;
    /**
     * The ID of this server in its category.
     */
    @Getter private final short id;
    /**
     * The host of the computer this server is running on.
     */
    @Getter(AccessLevel.PACKAGE) private final InetAddress host;
    /**
     * The port the network handler of this server should listen on (not the minecraft port!). Should be firewalled off:
     * it is unprotected!
     */
    private final int networkPort;

    @Override
    public SocketAddress getAddress() {
        return new InetSocketAddress(host, networkPort);
    }

    @Override
    public SocketAddress getBindAddress() {
        return new InetSocketAddress("0.0.0.0", networkPort);
    }
}
