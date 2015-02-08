/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec;

import at.yawk.accordion.codec.packet.Packet;

import java.util.function.Consumer;

/**
 * Channel that allows direct publish-subscribe of packets without manually working with named channels.
 *
 * @author yawkat
 */
public interface ObjectChannel<T> {
    /**
     * Publish the given message. Note that this message will also be handled by our subscribers.
     */
    void publish(T message);

    /**
     * Listen for the given packet type. Note that subclasses will not call this subscriber. Multiple subscribers for
     * the same packet type may be set, execution order is undefined and not necessarily in registration sequence or the
     * same for all packets (multiple received packets may invoke listeners in different order).
     */
    <P extends T> void subscribe(Class<P> clazz, Consumer<P> listener);
}
