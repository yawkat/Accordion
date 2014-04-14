package at.yawk.accordion.minecraft;

import at.yawk.accordion.minecraft.auto.API;
import at.yawk.accordion.minecraft.auto.ApiProvider;
import at.yawk.accordion.minecraft.auto.PluginBridge;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

/**
 * @author Yawkat
 */
public class AccordionBungee extends Plugin implements ApiProvider, Listener {
    public static final String NAME = "Accordion";

    private PluginBridge bridge;

    @Override
    public void onEnable() {
        // load bridge
        try {
            bridge = new PluginBridge(new Config(new File(getDataFolder(), "config.properties"),
                                                 ServerCategory.Default.BUNGEE,
                                                 getProxy().getConfig()
                                                           .getListeners()
                                                           .iterator()
                                                           .next()
                                                           .getHost()
                                                           .getPort()
            ));
        } catch (IOException e) {
            throw new IOError(e);
        }

        getProxy().getPluginManager().registerListener(this, this);

        // start after 5 seconds so we can be sure all depending plugins completed their work
        getProxy().getScheduler().schedule(this, bridge::start, 5, TimeUnit.SECONDS);
    }

    @Override
    public API getApi() {
        // must be loaded
        if (bridge == null) { throw new IllegalStateException(); }
        return bridge;
    }

    @EventHandler
    public void message(PluginMessageEvent event) {
        // peer discovery receiver
        if (event.getTag().equals(Constants.PEER_DISCOVERY_CHANNEL)) {
            event.setCancelled(true);

            if (event.getSender() instanceof net.md_5.bungee.api.connection.Server) {
                bridge.getPluginMessageHandler().accept(event.getData());
            }
        }
    }

    @EventHandler
    public void connect(ServerConnectedEvent event) {
        // use the new channel to bukkit to do some peer discovery
        bridge.useChannel(payload -> event.getServer().sendData(Constants.PEER_DISCOVERY_CHANNEL, payload));
    }
}
