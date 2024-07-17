package net.janrupf.ujr.example.glfw.bridge;

import net.janrupf.ujr.api.logger.UltralightLogLevel;
import net.janrupf.ujr.api.logger.UltralightLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggerBridge implements UltralightLogger {
    private static final Logger LOGGER = LogManager.getLogger("com.ultralight.Ultralight");

    @Override
    public void logMessage(UltralightLogLevel logLevel, String message) {
        Level translatedLevel = transateLogLevel(logLevel);
        LOGGER.log(translatedLevel, message);
    }

    /**
     * Helper function to translate from Ultralight's log levels to Log4j's log levels.
     *
     * @param logLevel the Ultralight log level to translate
     * @return the translated log level for Log4j
     */
    private Level transateLogLevel(UltralightLogLevel logLevel) {
        switch (logLevel) {
            // Map levels 1:1
            case ERROR:
                return Level.ERROR;
            case WARNING:
                return Level.WARN;
            case INFO:
                return Level.INFO;

            default:
                throw new RuntimeException("Unknown log level: " + logLevel);
        }
    }
}
