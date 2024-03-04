package org.kettingpowered.launcher.log.impl;

import net.minecrell.terminalconsole.TerminalConsoleAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.kettingpowered.launcher.log.LogLevel;
import org.kettingpowered.launcher.log.Logger;

import java.io.IOException;

public class Log4jImpl extends Logger {

    private static final LoggerContext CONTEXT = (LoggerContext) LogManager.getContext(false);
    private static final org.apache.logging.log4j.Logger LOGGER = CONTEXT.getLogger("Launcher");

    protected void _log(LogLevel level, String message) {
        LOGGER.log(level.toLog4jLevel(), message);
    }

    protected void _log(LogLevel level, String message, Object... args) {
        LOGGER.log(level.toLog4jLevel(), message, args);
    }

    protected void _log(LogLevel level, String message, Throwable throwable) {
        LOGGER.log(level.toLog4jLevel(), message, throwable);
    }

    protected void _marker(String marker, String message) {
        LOGGER.info(MarkerManager.getMarker(marker), message);
    }

    protected void _shutdown() {
        CONTEXT.terminate();
        try {
            TerminalConsoleAppender.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
