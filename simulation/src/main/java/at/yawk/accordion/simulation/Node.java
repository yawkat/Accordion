package at.yawk.accordion.simulation;

import at.yawk.accordion.minecraft.ServerCategory;
import at.yawk.accordion.minecraft.auto.PluginBridge;
import at.yawk.accordion.minecraft.auto.PluginConfig;
import at.yawk.accordion.p2p.Envelope;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;

/**
 * A single node in the virtual connection net.
 *
 * @author Yawkat
 */
@Log4j
@RequiredArgsConstructor
public class Node implements Consumer<Envelope> {
    @Getter private final String id;
    private final int port;
    private final ServerCategory category;

    @Getter private PluginBridge bridge = null;

    public void init() {
        bridge = new PluginBridge(new PluginConfig() {
            @Override
            public long getId(long defaultValue) {
                return defaultValue;
            }

            @Override
            public ServerCategory getType() {
                return category;
            }

            @Override
            public InetAddress getPublicAddress() {
                try {
                    return InetAddress.getByName("localhost");
                } catch (UnknownHostException e) {
                    throw new Error(e);
                }
            }

            @Override
            public int getNetworkPort() {
                return port;
            }
        });
    }

    public void start() {
        log.info(id + " - Start");
        bridge.start();
    }

    @Override
    public void accept(Envelope envelope) {
        log.info(id + " - Receive | " + envelope.getPacket());
    }
}
