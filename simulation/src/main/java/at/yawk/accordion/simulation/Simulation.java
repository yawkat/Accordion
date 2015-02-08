/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.simulation;

import at.yawk.accordion.Log;
import at.yawk.accordion.compression.SnappyCompressor;
import at.yawk.accordion.compression.VoidCompressor;
import at.yawk.accordion.distributed.LocalNode;
import at.yawk.accordion.distributed.Node;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

/**
 * @author yawkat
 */
public class Simulation {
    private static final int BASE_PORT = 5000;

    private final Map<Node, LocalNode> nodes = new HashMap<>();
    private Node[][] tiers = { new Node[2], new Node[1] };

    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        Logger.getRootLogger().addAppender(new ConsoleAppender(new SimpleLayout(), ConsoleAppender.SYSTEM_OUT));
        Logger.getRootLogger().setLevel(Level.DEBUG);

        Simulation simulation = new Simulation();
        simulation.populate();
        TimeUnit.SECONDS.sleep(1);

        simulation.nodes.get(simulation.tiers[0][0]).getConnectionManager().getChannel("test").subscribe(msg -> {
            int len = msg.readInt();
            byte[] blob = new byte[len];
            msg.readBytes(blob);
            Log.getDefaultLogger().info("Received: " + new String(blob));
        });
        TimeUnit.SECONDS.sleep(1);

        ByteBuf msg = Unpooled.buffer();
        byte[] text = "ping".getBytes();
        msg.writeInt(text.length);
        msg.writeBytes(text);

        simulation.nodes.get(simulation.tiers[0][1]).getConnectionManager().getChannel("test").publish(msg);
    }

    private void populate() throws UnknownHostException {
        InetAddress local = InetAddress.getLocalHost();

        int port = BASE_PORT;
        for (int tier = 0; tier < tiers.length; tier++) {
            int count = tiers[tier].length;
            for (int i = 0; i < count; i++) {
                Node node = new Node(new InetSocketAddress(local, port++), tier);
                LocalNode locNode = LocalNode.builder().compressor(SnappyCompressor.getInstance()).self(node).build();
                if (tier > 0) {
                    locNode.listen();
                }
                nodes.put(node, locNode);
                tiers[tier][i] = node;
            }
        }

        for (int tier = 0; tier < tiers.length - 1; tier++) {
            for (int i = 0; i < tiers[tier].length; i++) {
                int rem = new Random().nextInt(tiers[tier + 1].length);
                Log.getDefaultLogger().info("Linking " + tier + "." + i + " -> " + (tier + 1) + "." + rem);
                Node above = tiers[tier + 1][rem];
                nodes.get(tiers[tier][i]).addNodes(Stream.of(above));
            }
        }

        nodes.values().forEach(node -> node.getKnownNodes().forEach(node::connect));
    }
}
