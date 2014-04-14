package at.yawk.accordion.minecraft.auto;

import at.yawk.accordion.minecraft.ServerCategory;
import java.net.InetAddress;

/**
 * Individual node configuration.
 *
 * @author Yawkat
 */
public interface PluginConfig {
    /**
     * The ID of this server / node.
     */
    long getId(long defaultValue);

    /**
     * The type of this node.
     */
    ServerCategory getType();

    /**
     * Global address.
     */
    InetAddress getPublicAddress();

    /**
     * Global port.
     */
    int getNetworkPort();
}
