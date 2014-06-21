package at.yawk.accordion.minecraft;

import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

/**
 * @author yawkat
 */
public class AccordionBungee {
    private AccordionBungee() {}

    public static AccordionApi createApi(Plugin plugin) {
        InetSocketAddress host = plugin.getProxy().getConfig().getListeners().stream().findAny().get().getHost();
        AccordionApi api = new AccordionApi()
                .mcPort(host.getPort())
                .logger(plugin.getLogger())
                .listen(true)
                .tier(AccordionApi.DEFAULT_TIER_BUNGEE);
        // TODO reliable autostart
        plugin.getProxy().getScheduler().schedule(plugin, api::tryAutoStart, 3, TimeUnit.SECONDS);

        plugin.getProxy().getPluginManager().registerListener(plugin, new Listener() {
            @EventHandler
            public void onPluginMessage(PluginMessageEvent event) {
                // filter malicious peer packets from client
                if (event.getTag().equals(AccordionApi.PEER_DISCOVERY_PLUGIN_CHANNEL)) {
                    event.setCancelled(true);
                }
            }

            @EventHandler
            public void onSwitch(ServerSwitchEvent event) {
                if (!api.isAutomaticDiscovery()) {
                    return;
                }

                // send our known nodes to the server
                ByteBuf enc = api.getLocalNode().getKnownNodesEncoded();
                byte[] array = new byte[enc.readableBytes()];
                enc.readBytes(array);

                event.getPlayer().getServer().sendData(AccordionApi.PEER_DISCOVERY_PLUGIN_CHANNEL, array);
            }
        });

        return api;
    }
}
