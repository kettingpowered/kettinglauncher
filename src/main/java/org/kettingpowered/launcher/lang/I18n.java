package org.kettingpowered.launcher.lang;

import org.jetbrains.annotations.NotNull;
import org.kettingpowered.launcher.Main;
import org.kettingpowered.launcher.log.LogLevel;
import org.kettingpowered.launcher.log.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

public class I18n {

    private static final String LANG_PATH = "lang/";

    private static final Properties translations = new Properties();
    private static final Properties fallback = new Properties();

    private static Locale current;

    public static void load() {
        load(false);
    }

    public static void load(boolean silent) {
        if (current != null) return;
        silent = silent && !Main.DEBUG;

        current = Locale.getDefault();

        String lang = (current.getLanguage() + "_" + current.getCountry()).toLowerCase();
        loadFile(lang, translations, silent);

        //Load fallback translations
        if (!lang.equals("en_us"))
            loadFile("en_us", fallback, silent);

        if (!silent)
            Logger.log("Loaded %s translations", translations.size());
    }

    private static void loadFile(String langCode, Properties toAdd, boolean silent) {
        silent = silent && !Main.DEBUG;
        try (InputStream lang = I18n.class.getClassLoader().getResourceAsStream(LANG_PATH + langCode + ".properties")) {
            if (lang == null) {
                if (!silent) Logger.log(LogLevel.WARN, "Language file not found for %s, using default", langCode);
                return;
            }

            toAdd.load(lang);
        } catch (IOException io) {
            if (silent) return;
            Logger.log("Failed to load language file for " + langCode, io);
        }
    }

    public static String get(@NotNull String key) {
        Object translation = translations.get(key);
        if (translation == null) {
            if (fallback.isEmpty()) {
                Logger.log(LogLevel.ERROR, "Missing translation for " + key);
                return key;
            }

            translation = fallback.get(key);
            if (translation == null) {
                Logger.log(LogLevel.ERROR, "Missing translation for " + key);
                return key;
            }

            return translation.toString();
        }
        return translation.toString();
    }

    public static String get(@NotNull String key, Object... args) {
        return String.format(get(key), args);
    }

    public static void log(@NotNull String key) {
        Logger.log(get(key));
    }

    public static void log(@NotNull String key, Object... args) {
        Logger.log(get(key, args));
    }

    public static void logError(@NotNull String key) {
        Logger.log(LogLevel.ERROR, get(key));
    }

    public static void logError(@NotNull String key, Object... args) {
        Logger.log(LogLevel.ERROR, get(key, args));
    }

    public static void logDebug(@NotNull String key) {
        Logger.log(LogLevel.DEBUG, get(key));
    }

    public static void logDebug(@NotNull String key, Object... args) {
        Logger.log(LogLevel.DEBUG, get(key, args));
    }
}
