package at.yawk.accordion.minecraft;

import at.yawk.accordion.p2p.ConnectionPolicy;
import at.yawk.accordion.p2p.Recipient;
import at.yawk.accordion.p2p.RecipientRange;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

/**
 * ConnectionPolicy that has a specific RecipientRange of nodes that "control" the entire peer-to-peer network. The
 * nodes in this "backbone" will be heavily connected to other backbone nodes to ensure fast transfer. All nodes not
 * part of the "backbone" will be connected to nodes of the backbone (usually only one) and receive / send packets
 * through them. The non-backbone nodes rely on the backbone and cannot communicate without it.
 * <p/>
 * Backbones on the same host (computer) are preferred as to minimize bandwidth usage.
 *
 * @author Yawkat
 */
@RequiredArgsConstructor
public class BackboneConnectionPolicy implements ConnectionPolicy {
    private final RecipientRange backbone;
    private final int maximumConnectedTo;

    /**
     * @param maximumConnectedTo The maximum amount of backbone nodes a node should connect to.
     */
    public BackboneConnectionPolicy(int maximumConnectedTo) {
        this(ServerCategory.Default.BUNGEE, maximumConnectedTo);
    }

    public BackboneConnectionPolicy() {
        this(1);
    }

    @Override
    public boolean routePacketThrough(Recipient self, Recipient remote, RecipientRange packetReceiver) {
        // Routes the packet to all backbone nodes and the receiver nodes.
        return packetReceiver.contains(remote) || backbone.contains(remote);
    }

    @Override
    public Stream<Recipient> listPossiblePeers(Recipient self, Collection<Recipient> possibilities) {
        List<Recipient> filtered = possibilities.stream()
                // only connect to backbone
                .filter(backbone::contains).collect(Collectors.toList());

        // mix
        Collections.shuffle(filtered);

        if (!backbone.contains(self)) {
            // if this is not a backbone server, prefer connecting to nodes on the same host to minimize cross-server
            // bandwidth.
            Collections.sort(filtered,
                             Comparator.comparing(r -> !((Server) r).getHost().equals(((Server) r).getHost())));
        }
        return filtered.stream();
    }

    @Override
    public boolean listenForPeers(Recipient self, int connectedToSelfCount, int selfConnectedToCount) {
        // Backbone always listens, other servers never do.
        return backbone.contains(self);
    }

    @Override
    public boolean searchForPeers(Recipient self, int connectedToSelfCount, int selfConnectedToCount) {
        // non-backbone servers only maintain maximumConnectedTo connections, backbones connect infinitely.
        return backbone.contains(self) || selfConnectedToCount < maximumConnectedTo;
    }
}
