package at.yawk.accordion.minecraft.example;

import at.yawk.accordion.minecraft.MinecraftNetManager;
import at.yawk.accordion.minecraft.Server;
import at.yawk.accordion.minecraft.ServerCategory;
import at.yawk.accordion.minecraft.ServerLoader;
import at.yawk.accordion.p2p.Envelope;
import at.yawk.accordion.packet.Packet;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import javax.xml.parsers.ParserConfigurationException;
import lombok.extern.log4j.Log4j;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.xml.sax.SAXException;

/**
 * Example bungee plugin.
 *
 * @author Yawkat
 */
@Log4j
public class AccordionBungee extends Plugin implements Consumer<Envelope> {
    private MinecraftNetManager netManager;

    /**
     * Mostly copied from AccordionBukkit, see that file for details.
     */
    @Override
    public void onEnable() {
        List<Server> servers;
        try (InputStream stream = getResourceAsStream("servers.xml")) {
            servers = new ServerLoader().loadPeersFromXml(stream);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new Error(e);
        }

        short id;
        try {
            id = (short) ConfigurationProvider.getProvider(YamlConfiguration.class)
                                              .load(new File(getDataFolder(), "config.yml"))
                                              .getInt("id");
        } catch (IOException e) {
            throw new IOError(e);
        }
        Server self = servers.stream()
                             .filter(s -> s.getCategory() == ServerCategory.Default.BUNGEE)
                             .filter(s -> s.getId() == id)
                             .findAny()
                             .get();

        netManager = new MinecraftNetManager(self, this);
        netManager.addPeers(servers);
        netManager.registerPacket(1, PingPacket.class);
        netManager.registerPacket(2, PongPacket.class);

        netManager.start();

        log.info("Accordion bungee launched.");
    }

    @Override
    public void accept(Envelope envelope) {
        Packet packet = envelope.getPacket();
        getLogger().info("Received Packet: " + packet);

        if (packet instanceof PingPacket) {
            getLogger().info("Ping");
            netManager.broadcast(new PongPacket(((PingPacket) packet).getPayload()));
        } else if (packet instanceof PongPacket) {
            getLogger().info("Pong " + ((PongPacket) packet).getPayload());
        }
    }
}
