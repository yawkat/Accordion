/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.minecraft;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * @author yawkat
 */
public class AccordionBukkit {
    private AccordionBukkit() {}

    public static AccordionApi createApi(Plugin plugin) {
        try {
            AccordionApi api = new AccordionApi()
                    .mcPort(Bukkit.getPort())
                    // use plugin logger
                    .logger(plugin.getLogger())
                    .listenAddress(InetAddress.getByName(Bukkit.getIp()))
                    // do not listen on bukkit
                    .listen(false)
                    .tier(AccordionApi.DEFAULT_TIER_BUKKIT);
            // auto start on first tick (bukkit load complete)
            plugin.getServer().getScheduler().runTask(plugin, api::tryAutoStart);
            // peer discovery
            plugin.getServer().getMessenger()
                    .registerIncomingPluginChannel(plugin, AccordionApi.PEER_DISCOVERY_PLUGIN_CHANNEL,
                                                   (channel, player, data) -> {
                                                       if (!api.isAutomaticDiscovery()) {
                                                           return;
                                                       }

                                                       // new nodes received
                                                       ByteBuf wrapped = Unpooled.wrappedBuffer(data);
                                                       api.getLocalNode().loadEncodedNodes(wrapped);
                                                   });
            return api;
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Invalid bukkit IP", e);
        }
    }
}
