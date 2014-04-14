package at.yawk.accordion.minecraft.example;

import at.yawk.accordion.minecraft.AccordionBungee;
import at.yawk.accordion.minecraft.auto.API;
import lombok.extern.log4j.Log4j;
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
        // get the api
        API api = ((AccordionBungee) getProxy().getPluginManager().getPlugin(AccordionBungee.NAME)).getApi();

        // register ping with handler
        api.registerPacket("example.ping", PingPacket.class, received -> {
            // Print "ping" to console and reply with a PongPacket.
            getLogger().info("Ping");
            api.broadcast(new PongPacket(received.getPayload()));
        });

        // register pong with handler
        api.registerPacket("example.pong", PongPacket.class, received -> {
            // Print "pong" to console.
            getLogger().info("Pong " + received.getPayload());
        });
    }
}
