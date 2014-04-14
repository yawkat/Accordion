package at.yawk.accordion.simulation;

import at.yawk.accordion.minecraft.ServerCategory;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Yawkat
 */
public class NodeManager {
    private final Set<Node> nodes = new HashSet<>();

    public static void main(String[] args) throws InterruptedException {
        new NodeManager().runTests();
    }

    private void runTests() throws InterruptedException {
        makeNodes();
        nodes.forEach(Node::start);

        link(node("bungee0"), node("bukkit0"));
        TimeUnit.SECONDS.sleep(2);
        node("bungee0").getBridge().broadcast(new HelloPacket());
        TimeUnit.SECONDS.sleep(2);

        link(node("bungee0"), node("bukkit1"));
        TimeUnit.SECONDS.sleep(2);
        node("bungee0").getBridge().broadcast(new HelloPacket());
        TimeUnit.SECONDS.sleep(2);

        link(node("bungee1"), node("bukkit1"));
        TimeUnit.SECONDS.sleep(2);
        node("bungee0").getBridge().broadcast(new HelloPacket());
        TimeUnit.SECONDS.sleep(2);
    }

    /**
     * Get a node by ID.
     */
    private Node node(String id) {
        return nodes.stream().filter(n -> n.getId().equals(id)).findAny().get();
    }

    /**
     * Prepare the virtual network with a few nodes.
     */
    private void makeNodes() {
        int port = 2000;

        makeNode("bungee0", port++, ServerCategory.Default.BUNGEE);
        makeNode("bungee1", port++, ServerCategory.Default.BUNGEE);
        makeNode("bukkit0", port++, ServerCategory.Default.BUKKIT);
        makeNode("bukkit1", port++, ServerCategory.Default.BUKKIT);
    }

    /**
     * Create a virtual node.
     */
    private void makeNode(String name, int port, ServerCategory category) {
        Node node = new Node(name, port, category);
        nodes.add(node);
        node.init();

        node.getBridge()
            .registerPacket("simulate.hello",
                            HelloPacket.class,
                            received -> System.out.println(name + " - Received: Hello"));
    }

    /**
     * Create a fake plugin channel between two nodes so they can use each other for peer discovery.
     */
    private void link(Node n1, Node n2) {
        n1.getBridge().useChannel(n2.getBridge().getPluginMessageHandler());
        n2.getBridge().useChannel(n1.getBridge().getPluginMessageHandler());
    }
}
