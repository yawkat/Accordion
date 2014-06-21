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
