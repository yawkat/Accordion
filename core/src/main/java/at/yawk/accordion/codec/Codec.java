/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec;

/**
 * Bidirectional converter from an encoded type to a decoded type.
 *
 * @author yawkat
 */
public interface Codec<T, U> {
    /**
     * Encode the given object.
     */
    T encode(U message);

    /**
     * Decode the given object.
     */
    U decode(T encoded);
}
