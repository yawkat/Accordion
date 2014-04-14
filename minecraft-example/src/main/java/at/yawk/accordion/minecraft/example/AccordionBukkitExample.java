package at.yawk.accordion.minecraft.example;

import at.yawk.accordion.minecraft.AccordionBukkit;
import at.yawk.accordion.minecraft.auto.API;
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
        // get the api
        API api = getPlugin(AccordionBukkit.class).getApi();

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

        // register test command
        getCommand("ping").setExecutor((sender, command, lbl, args) -> {
            api.broadcast(new PingPacket(Integer.parseInt(args[0])));
            return true;
        });
    }
}
