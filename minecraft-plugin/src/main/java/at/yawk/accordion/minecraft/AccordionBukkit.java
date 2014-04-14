package at.yawk.accordion.minecraft;

import at.yawk.accordion.minecraft.auto.API;
import at.yawk.accordion.minecraft.auto.ApiProvider;
import at.yawk.accordion.minecraft.auto.PluginBridge;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author Yawkat
 */
public class AccordionBukkit extends JavaPlugin implements ApiProvider, Listener {
    private PluginBridge bridge;

    @SuppressWarnings("unchecked")
    @Override
    public void onEnable() {
        // load bridge
        try {
            bridge = new PluginBridge(new Config(new File(getDataFolder(), "config.properties"),
                                                 ServerCategory.Default.BUNGEE,
                                                 Bukkit.getPort()));
        } catch (IOException e) {
            throw new IOError(e);
        }

        // prepare peer discovery receiver
        getServer().getMessenger()
                   .registerIncomingPluginChannel(this,
                                                  Constants.PEER_DISCOVERY_CHANNEL,
                                                  (channel, player, message) -> bridge.getPluginMessageHandler()
                                                                                      .accept(message)
                                                 );
        // prepare peer discovery sender
        getServer().getMessenger().registerOutgoingPluginChannel(this, Constants.PEER_DISCOVERY_CHANNEL);

        getServer().getPluginManager().registerEvents(this, this);

        // start after 5 seconds so we can be sure all depending plugins completed their work
        new BukkitRunnable() {
            @Override
            public void run() {
                bridge.start();
            }
        }.runTaskLaterAsynchronously(this, 100);
    }

    @Override
    public API getApi() {
        // must be loaded
        if (bridge == null) { throw new IllegalStateException(); }
        return bridge;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void joined(PlayerJoinEvent event) {
        // use the new channel to bungee to do some peer discovery
        bridge.useChannel(payload -> event.getPlayer()
                                          .sendPluginMessage(AccordionBukkit.this,
                                                             Constants.PEER_DISCOVERY_CHANNEL,
                                                             payload));
    }
}
