package at.yawk.accordion.minecraft.example;

import at.yawk.accordion.minecraft.MinecraftNetManager;
import at.yawk.accordion.minecraft.Server;
import at.yawk.accordion.minecraft.ServerCategory;
import at.yawk.accordion.minecraft.ServerLoader;
import at.yawk.accordion.p2p.Envelope;
import at.yawk.accordion.packet.Packet;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import javax.xml.parsers.ParserConfigurationException;
import lombok.extern.log4j.Log4j;
import org.bukkit.plugin.java.JavaPlugin;
import org.xml.sax.SAXException;

/**
 * Example bukkit plugin.
 * <p/>
 * Note that the configuration file "config.yml" in the plugins/Accordion folder must contain an ID that exists in the
 * servers.xml file.
 *
 * @author Yawkat
 */
@Log4j
public class AccordionBukkit extends JavaPlugin implements Consumer<Envelope> {
    private MinecraftNetManager netManager;

    @Override
    public void onEnable() {
        // load the servers in the network from servers.xml.
        List<Server> servers;
        try (InputStream stream = getResource("servers.xml")) {
            servers = new ServerLoader().loadPeersFromXml(stream);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new Error(e);
        }

        // load the own server ID from the config file so we can find it in the servers.xml.
        short id = (short) getConfig().getInt("id");
        // Find this server in the servers.xml
        Server self = servers.stream()
                // find a server that is both a bukkit server...
                .filter(s -> s.getCategory() == ServerCategory.Default.BUKKIT)
                        // ... and has the same ID as us
                .filter(s -> s.getId() == id)
                        // must be us!
                .findAny().get();

        // Create the network manager. The first argument is this server instance so other servers know who we are,
        // the second parameter is the packet handler that gets called when we receive a message from the network.
        netManager = new MinecraftNetManager(self, this);
        // add the other servers to it so we can communicate with them
        netManager.addPeers(servers);
        // register the two packets we have right now. Each must have its own ID.
        netManager.registerPacket(1, PingPacket.class);
        netManager.registerPacket(2, PongPacket.class);

        // start the network manager, we will soon be connected to the other servers!
        netManager.start();

        // example command that sends a message to the network. Usage: /ping <number>. All other servers will print
        // "Ping" in their console once. After that, they reply with a PongPacket that gets printed in the console of
        // all other servers.
        getCommand("ping").setExecutor((sender, command, c, args) -> {
            // send a packet to ALL servers in the network.
            netManager.broadcast(new PingPacket(Integer.parseInt(args[0])));
            return true;
        });

        log.info("Accordion bukkit launched.");
    }

    /**
     * Called when a message is received from the network.
     */
    @Override
    public void accept(Envelope envelope) {
        Packet packet = envelope.getPacket();
        getLogger().info("Received Packet: " + packet);

        if (packet instanceof PingPacket) {
            // Print "ping" to console and reply with a PongPacket.
            getLogger().info("Ping");
            netManager.broadcast(new PongPacket(((PingPacket) packet).getPayload()));
        } else if (packet instanceof PongPacket) {
            // Print "pong" to console.
            getLogger().info("Pong " + ((PongPacket) packet).getPayload());
        }
    }
}
