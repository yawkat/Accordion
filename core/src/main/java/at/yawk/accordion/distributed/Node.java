/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.distributed;

import at.yawk.accordion.codec.ByteCodec;
import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

/**
 * Global identity of a network node.
 *
 * @author yawkat
 */
@Value
public class Node {
    /**
     * Codec for easier use of #read and #write.
     */
    @Getter
    private static ByteCodec<Node> codec = new ByteCodec<Node>() {
        @Override
        public Node decode(ByteBuf encoded) {
            return read(encoded);
        }

        @Override
        public void encode(ByteBuf target, Node message) {
            message.write(target);
        }
    };

    /**
     * The externally available address of this node.
     */
    @NonNull
    private final InetSocketAddress address;
    /**
     * The tier / group this node belongs to.
     */
    private final int tier;

    /**
     * Serialize this node to a given ByteBuf.
     */
    void write(ByteBuf to) {
        // port
        to.writeInt(address.getPort());
        // IP
        InternalProtocol.writeByteArray(to, address.getAddress().getAddress());
        // tier
        to.writeInt(tier);
    }

    /**
     * Deserialize a node from a stream.
     */
    static Node read(ByteBuf from) {
        // port
        int port = from.readInt();
        // IP
        byte[] address = InternalProtocol.readByteArray(from);
        // tier
        int tier = from.readInt();
        try {
            // build
            return new Node(new InetSocketAddress(InetAddress.getByAddress(address), port), tier);
        } catch (UnknownHostException e) {
            // illegal length IP should never happen if written by #write
            throw new Error("Invalid address " + Arrays.toString(address), e);
        }
    }
}
