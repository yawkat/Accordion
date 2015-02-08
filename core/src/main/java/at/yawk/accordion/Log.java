/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion;

import java.util.function.Supplier;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging helper used by accordion.
 *
 * @author yawkat
 */
public class Log {
    /**
     * Flag whether we should print full debug messages. This is used in addition to Logger.isDebugEnabled for
     * compatibility.
     */
    @Setter private static boolean debug = false;

    private Log() {}

    /**
     * Get the default logger used by accordion if no other logger is specified.
     */
    public static Logger getDefaultLogger() {
        return LoggerFactory.getLogger("at.yawk.accordion");
    }

    /**
     * Print a debug message if debugging is enabled.
     */
    public static void debug(Logger logger, Supplier<Object> message) {
        if (isDebug(logger)) {
            logger.debug(String.valueOf(message.get()));
        }
    }

    /**
     * Print an info message if info logging is enabled.
     */
    public static void info(Logger logger, Supplier<Object> message) {
        if (logger.isInfoEnabled()) {
            logger.info(String.valueOf(message.get()));
        }
    }

    /**
     * Returns whether the given logger has debug enabled in addition to the accordion debug flag.
     */
    public static boolean isDebug(Logger logger) {
        return debug && logger.isDebugEnabled();
    }
}
