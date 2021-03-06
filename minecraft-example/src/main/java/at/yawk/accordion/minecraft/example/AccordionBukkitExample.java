/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.minecraft.example;

import at.yawk.accordion.minecraft.AccordionApi;
import at.yawk.accordion.minecraft.AccordionBukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Example bukkit plugin.
 *
 * @author Yawkat
 */
public class AccordionBukkitExample extends JavaPlugin {
    @SuppressWarnings("CodeBlock2Expr")
    @Override
    public void onEnable() {
        AccordionApi api = AccordionBukkit.createApi(this);

        api.<PingPacket>subscribe(PingPacket.class, packet -> {
            getLogger().info("Ping");
            api.publish(new PongPacket(packet.getPayload()));
        });

        api.<PongPacket>subscribe(PongPacket.class, packet -> {
            getLogger().info("Pong " + packet.getPayload());
        });

        // register test command
        getCommand("ping").setExecutor((sender, command, lbl, args) -> {
            api.publish(new PingPacket(Integer.parseInt(args[0])));
            return true;
        });
    }
}
