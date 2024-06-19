package org.kettingpowered.launcher.log;

import org.kettingpowered.launcher.Main;
import org.kettingpowered.launcher.lang.I18n;
import org.kettingpowered.launcher.log.impl.SysoutImpl;

public abstract class Logger {

    private static Logger impl = new SysoutImpl();

    public static void setImpl(Logger impl) {
        if (Logger.impl != null) Logger.impl._shutdown();
        Logger.impl = impl;
        I18n.logDebug("logger.impl_changed", impl.getClass().getName());
    }

    public static void marker(String marker, String message) {
        impl._marker(marker.toUpperCase(), message);
    }

    public static void log(String message) {
        log(LogLevel.INFO, message);
    }
    public static void log(String message, Object... args) {
        log(LogLevel.INFO, message, args);
    }

    public static void log(LogLevel level, String message) {
        if (level == LogLevel.DEBUG && !Main.DEBUG)
            return;

        impl._log(level, message);
    }
    public static void log(LogLevel level, String message, Object... args) {
        if (level == LogLevel.DEBUG && !Main.DEBUG)
            return;

        impl._log(level, message, args);
    }
    public static void log(String message, Throwable throwable) {
        impl._log(LogLevel.ERROR, message, throwable);
    }

    protected abstract void _log(LogLevel level, String message);
    protected abstract void _log(LogLevel level, String message, Object... args);
    protected abstract void _log(LogLevel level, String message, Throwable throwable);
    protected void _marker(String marker, String message) {
        //Default implementation does nothing
        _log(LogLevel.INFO, message);
    }
    protected void _shutdown() {}
}
