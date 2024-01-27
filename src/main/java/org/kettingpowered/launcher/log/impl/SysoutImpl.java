package org.kettingpowered.launcher.log.impl;

import org.kettingpowered.launcher.log.LogLevel;
import org.kettingpowered.launcher.log.Logger;

import java.io.PrintStream;

public class SysoutImpl extends Logger {

    protected void _log(LogLevel level, String message) {
        getPrintStream(level).println(message);
    }

    protected void _log(LogLevel level, String message, Object... args) {
        getPrintStream(level).printf(message + "%n", args);
    }

    protected void _log(LogLevel level, String message, Throwable throwable) {
        PrintStream stream = getPrintStream(level);
        stream.println(message);
        throwable.printStackTrace(stream);
    }

    private PrintStream getPrintStream(LogLevel level) {
        return switch (level) {
            case WARN, ERROR -> System.err;
            default -> System.out;
        };
    }
}
