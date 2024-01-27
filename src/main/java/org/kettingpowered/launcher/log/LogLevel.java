package org.kettingpowered.launcher.log;

import org.apache.logging.log4j.Level;

public enum LogLevel {
    INFO, WARN, ERROR, DEBUG;

    public Level toLog4jLevel() {
        return switch (this) {
            case INFO -> Level.INFO;
            case WARN -> Level.WARN;
            case ERROR -> Level.ERROR;
            case DEBUG -> Level.DEBUG;
        };
    }
}
