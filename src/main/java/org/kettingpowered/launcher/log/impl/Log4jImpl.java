package org.kettingpowered.launcher.log.impl;

import org.apache.logging.log4j.LogManager;
import org.kettingpowered.launcher.log.LogLevel;
import org.kettingpowered.launcher.log.Logger;

public class Log4jImpl extends Logger {

    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger("Launcher");

    protected void _log(LogLevel level, String message) {
        LOGGER.log(level.toLog4jLevel(), message);
    }

    protected void _log(LogLevel level, String message, Object... args) {
        LOGGER.log(level.toLog4jLevel(), message, args);
    }

    protected void _log(LogLevel level, String message, Throwable throwable) {
        LOGGER.log(level.toLog4jLevel(), message, throwable);
    }
}
