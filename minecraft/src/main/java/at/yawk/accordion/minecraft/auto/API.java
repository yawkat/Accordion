package at.yawk.accordion.minecraft.auto;

import at.yawk.accordion.minecraft.ServerCategory;
import at.yawk.accordion.p2p.RecipientRange;
import at.yawk.accordion.packet.Packet;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * API for Accordion.
 *
 * @author Yawkat
 */
public interface API {
    /**
     * Register a new packet.
     *
     * @param id       The ID of the packet, no length limitations. Try to use a distinct name so there are no
     *                 collisions with other plugins!
     * @param creator  Factory Supplier to create a new instance of this packet.
     * @param type     Base class that all packets of this type extend.
     * @param listener Listener that should be called when a packet of this type is received.
     */
    <P extends Packet> void registerPacket(String id, Supplier<P> creator, Class<P> type, Consumer<P> listener);

    /**
     * Register a new packet. Requires the type class to have an empty constructor.
     *
     * @param id       The ID of the packet, no length limitations. Try to use a distinct name so there are no
     *                 collisions with other plugins!
     * @param type     Base class that all packets of this type extend.
     * @param listener Listener that should be called when a packet of this type is received.
     */
    default <P extends Packet> void registerPacket(String id, Class<P> type, Consumer<P> listener) {
        try {
            Constructor<P> constructor = type.getConstructor();
            constructor.setAccessible(true);
            registerPacket(id, () -> {
                try {
                    return constructor.newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new Error(e);
                }
            }, type, listener);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Register a new packet. Requires the type class to have an empty constructor. Uses the type class name as the ID.
     *
     * @param type     Base class that all packets of this type extend.
     * @param listener Listener that should be called when a packet of this type is received.
     */
    default <P extends Packet> void registerPacket(Class<P> type, Consumer<P> listener) {
        registerPacket(type.getName(), type, listener);
    }

    /**
     * Register a new server category. All Recipients that are given to the given RecipientRange can be assumed to be
     * Server instances.
     *
     * @return the category that can be used with #transmit.
     */
    ServerCategory registerCategory(String name, RecipientRange category);

    /**
     * Send a packet to all servers in the given category. Check out ServerCategory.Default for some default
     * categories.
     */
    void transmit(Packet packet, ServerCategory recipients);

    /**
     * Broadcast a packet to all servers in the network.
     */
    default void broadcast(Packet packet) {
        transmit(packet, ServerCategory.Default.ALL);
    }
}
