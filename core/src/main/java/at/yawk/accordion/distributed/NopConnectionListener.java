/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.distributed;

import lombok.Getter;

/**
 * ConnectionListener implementation that does nothing.
 *
 * @author yawkat
 */
public class NopConnectionListener implements ConnectionListener {
    /**
     * The singleton instance.
     */
    @Getter private static final ConnectionListener instance = new NopConnectionListener();

    /**
     * getInstance alias that can be used as a ConnectionListenerFactory via method reference.
     */
    @SuppressWarnings("UnusedParameters")
    public static ConnectionListener getInstance(LocalNode localNode) {
        return getInstance();
    }

    private NopConnectionListener() {}
}
