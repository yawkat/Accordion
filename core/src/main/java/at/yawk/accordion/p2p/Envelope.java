package at.yawk.accordion.p2p;

import at.yawk.accordion.packet.Packet;
import at.yawk.accordion.packet.PacketManager;
import at.yawk.accordion.packet.PacketType;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.Random;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Envelope is the wrapper class for Packet. It contains all data actually sent between nodes.
 *
 * @author Yawkat
 */
@Getter(AccessLevel.PACKAGE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Envelope {
    /**
     * Generator Random for #id.
     */
    private static final Random ID_GENERATOR = new Random();

    /**
     * The sender of this Envelope. Only set if this Envelope was received through a Session. Used internally to avoid
     * sending the same packet back to the original sender.
     */
    private final Optional<Recipient> sender;
    /**
     * The PacketManager used for serialization / deserialization.
     */
    private final PacketManager packetManager;
    /**
     * The unique, randomly generated ID of this packet. Used internally to avoid duplicate packet handling.
     */
    private final long id;
    /**
     * The range of recipients that should be able to receive and handle this node. If a Recipient is in this range and
     * connected to the network, it is guaranteed to receive this Envelope eventually. Other peers might still receive
     * it for further transmission but should not handle it.
     */
    private final RecipientRange recipients;
    /**
     * The cache of the serialized packet. At least one of packetCache and this payloadCache should exist, the other
     * will be calculated when necessary.
     */
    private Optional<ByteBuf> payloadCache = Optional.empty();
    /**
     * The cache of the deserialized packet. At least one of packetCache and this payloadCache should exist, the other
     * will be calculated when necessary.
     */
    private Optional<Packet> packetCache = Optional.empty();

    /**
     * Create a new envelope.
     *
     * @param packetManager The packet manager.
     * @param recipients    The recipients that should receive this packet.
     * @param packet        The packet.
     * @return The finished envelope.
     */
    public static Envelope create(@NonNull PacketManager packetManager,
                                  @NonNull RecipientRange recipients,
                                  @NonNull Packet packet) {
        Envelope envelope = new Envelope(Optional.empty(), packetManager, ID_GENERATOR.nextLong(), recipients);
        envelope.packetCache = Optional.of(packet);
        return envelope;
    }

    /**
     * Create an envelope that was received from another node.
     *
     * @param sender        The sender of this Envelope.
     * @param packetManager The packet manager.
     * @param id            The unique ID of this Envelope.
     * @param recipients    The recipients that should receive this packet.
     * @param payload       The serialized packet.
     * @return The finished envelope.
     */
    static Envelope create(@NonNull Recipient sender,
                           @NonNull PacketManager packetManager,
                           long id,
                           @NonNull RecipientRange recipients,
                           @NonNull ByteBuf payload) {
        Envelope envelope = new Envelope(Optional.of(sender), packetManager, id, recipients);
        envelope.payloadCache = Optional.of(payload);
        return envelope;
    }

    /**
     * Returns the content of this Envelope. Deserializes if necessary.
     */
    public synchronized Packet getPacket() {
        if (!packetCache.isPresent()) {
            packetCache = Optional.of(packetManager.readPacket(payloadCache.get()));
        }
        return packetCache.get();
    }

    /**
     * Returns the serialized content of this Envelope or calculates it if it is unknown.
     */
    synchronized ByteBuf getPayload() {
        if (!payloadCache.isPresent()) {
            payloadCache = Optional.of(packetManager.writePacket(packetCache.get()));
        }
        ByteBuf payload = payloadCache.get().copy();
        payload.resetReaderIndex();
        return payload;
    }

    /**
     * Returns the PacketType of the packet in this envelope.
     */
    public PacketType<?> getPacketType() {
        return packetManager.findPacketType(getPacket());
    }
}
