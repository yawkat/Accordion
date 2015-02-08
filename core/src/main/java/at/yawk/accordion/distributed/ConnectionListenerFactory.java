/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.distributed;

/**
 * Functional interface to create a ConnectionListener for a LocalNode.
 *
 * @author yawkat
 */
@FunctionalInterface
public interface ConnectionListenerFactory {
    /**
     * Create a new ConnectionListener for the specific LocalNode.
     */
    ConnectionListener createConnectionListener(LocalNode localNode);
}
