/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion;

import at.yawk.accordion.codec.Codec;

/**
 * Messenger that provides unique channels by name.
 *
 * @author yawkat
 */
@FunctionalInterface
public interface Messenger<T> {
    Channel<T> getChannel(String name);

    /**
     * Transform this channel using the given codec.
     */
    default <U> Messenger<U> decode(Codec<T, U> codec) {
        return name -> getChannel(name).decode(codec);
    }
}
