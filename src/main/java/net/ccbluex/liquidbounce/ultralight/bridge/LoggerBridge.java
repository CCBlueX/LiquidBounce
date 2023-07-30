/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.ultralight.bridge;

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
