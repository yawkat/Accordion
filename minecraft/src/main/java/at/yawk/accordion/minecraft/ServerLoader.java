package at.yawk.accordion.minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Helper class to load Server instances from files (only XML right now).
 *
 * @author Yawkat
 */
public class ServerLoader {
    public List<Server> loadPeersFromXml(Path xmlFile) throws IOException, ParserConfigurationException, SAXException {
        try (InputStream stream = Files.newInputStream(xmlFile)) {
            return loadPeersFromXml(stream);
        }
    }

    public List<Server> loadPeersFromXml(InputStream stream)
            throws ParserConfigurationException, IOException, SAXException {
        List<Server> servers = new ArrayList<>();

        NodeList hosts = DocumentBuilderFactory.newInstance()
                                                  .newDocumentBuilder()
                                                  .parse(stream)
                                                  .getDocumentElement()
                                                  .getChildNodes();
        for (int i = 0; i < hosts.getLength(); i++) {
            Node hostNode = hosts.item(i);
            if (hostNode.getNodeType() == Node.ELEMENT_NODE) {
                NodeList serverNodes = hostNode.getChildNodes();
                String host = hostNode.getAttributes().getNamedItem("host").getNodeValue();
                for (int j = 0; j < serverNodes.getLength(); j++) {
                    Node serverNode = serverNodes.item(j);
                    if (serverNode.getNodeType() == Node.ELEMENT_NODE) {
                        String type = serverNode.getAttributes().getNamedItem("type").getNodeValue();
                        String id = serverNode.getAttributes().getNamedItem("id").getNodeValue();
                        String port = serverNode.getAttributes().getNamedItem("port").getNodeValue();
                        servers.add(Server.create(ServerCategory.Default.valueOf(type),
                                                  Short.parseShort(id),
                                                  InetAddress.getByName(host),
                                                  Integer.parseInt(port)));
                    }
                }
            }
        }
        return servers;
    }
}
