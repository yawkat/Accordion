package at.yawk.accordion.minecraft;

import at.yawk.accordion.minecraft.auto.PluginConfig;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import lombok.Getter;

/**
 * Config loader used by both API plugins.
 *
 * @author Yawkat
 */
class Config implements PluginConfig {
    private final File file;
    @Getter private final ServerCategory type;
    private final int minecraftPort;
    private final Properties properties;

    public Config(File file, ServerCategory type, int minecraftPort) throws IOException {
        this.file = file;
        this.type = type;
        this.minecraftPort = minecraftPort;
        this.properties = new Properties();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                properties.load(reader);
            }
        }
    }

    @Override
    public long getId(long defaultValue) {
        return Long.parseLong(getProperty("id", Long.toString(defaultValue)));
    }

    @Override
    public InetAddress getPublicAddress() {
        try {
            return InetAddress.getByName(getProperty("address", "127.0.0.1"));
        } catch (UnknownHostException e) {
            throw new Error(e);
        }
    }

    @Override
    public int getNetworkPort() {
        return Integer.parseInt(getProperty("port", Integer.toString(minecraftPort + 1315)));
    }

    private String getProperty(String name, String def) {
        if (!properties.containsKey(name)) {
            properties.put(name, def);
            save();
        }
        return String.valueOf(properties.get(name));
    }

    private void save() {
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            properties.store(writer, "Accordion configuration file");
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}
