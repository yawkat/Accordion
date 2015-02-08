/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.minecraft;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.impl.JDK14LoggerFactory;

/**
 * @author yawkat
 */
class JavaLogging {
    private static final ILoggerFactory FACTORY = new JDK14LoggerFactory();

    private JavaLogging() {}

    /**
     * Create a slf4j logger that writes to the given java logger.
     */
    static Logger fromJavaLogger(java.util.logging.Logger javaLogger) {
        return FACTORY.getLogger(javaLogger.getName());
    }
}
