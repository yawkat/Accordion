/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.Channel;
import at.yawk.accordion.Messenger;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * @author yawkat
 */
public class TestMessenger implements Messenger<ByteBuf> {
    @Override
    public Channel<ByteBuf> getChannel(String name) {
        return new Channel<ByteBuf>() {
            @Override
            public void publish(ByteBuf message) {
                byte[] data = Arrays.copyOf(message.array(), message.readableBytes());
                System.out.println(Arrays.toString(data));
                System.out.println(new String(data, StandardCharsets.UTF_8));
            }

            @Override
            public void subscribe(Consumer<ByteBuf> listener) {}
        };
    }
}
