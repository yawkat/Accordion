/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.minecraft.example;

import at.yawk.accordion.minecraft.AccordionApi;
import at.yawk.accordion.minecraft.AccordionBungee;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * Example bungee plugin.
 *
 * @author Yawkat
 */
public class AccordionBungeeExample extends Plugin {
    @SuppressWarnings("CodeBlock2Expr")
    @Override
    public void onEnable() {
        AccordionApi api = AccordionBungee.createApi(this);

        api.<PingPacket>subscribe(PingPacket.class, packet -> {
            getLogger().info("Ping");
            api.publish(new PongPacket(packet.getPayload()));
        });

        api.<PongPacket>subscribe(PongPacket.class, packet -> {
            getLogger().info("Pong " + packet.getPayload());
        });
    }
}
