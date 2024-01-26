package org.kettingpowered.launcher.lang;

import org.jetbrains.annotations.NotNull;
import org.kettingpowered.launcher.Main;

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
            System.out.println("Loaded " + translations.size() + " translations");
    }

    private static void loadFile(String langCode, Properties toAdd, boolean silent) {
        silent = silent && !Main.DEBUG;
        try (InputStream lang = I18n.class.getClassLoader().getResourceAsStream(LANG_PATH + langCode + ".properties")) {
            if (lang == null) {
                if (!silent) System.out.println("Language file not found for " + langCode + ", using default");
                return;
            }

            toAdd.load(lang);
        } catch (IOException io) {
            if (silent) return;
            System.err.println("Failed to load language file for " + langCode);
            io.printStackTrace();
        }
    }

    public static String get(@NotNull String key) {
        String translation = translations.get(key).toString();
        if (translation == null) {
            if (fallback.isEmpty()) {
                System.err.println("Missing translation for " + key);
                return key;
            }

            translation = fallback.get(key).toString();
            if (translation == null) {
                System.err.println("Missing translation for " + key);
                return key;
            }

            return translation;
        }
        return translation;
    }

    public static String get(@NotNull String key, Object... args) {
        return String.format(get(key), args);
    }

    public static void log(@NotNull String key) {
        System.out.println(get(key));
    }

    public static void log(@NotNull String key, Object... args) {
        System.out.println(get(key, args));
    }

    public static void logError(@NotNull String key) {
        System.err.println(get(key));
    }

    public static void logError(@NotNull String key, Object... args) {
        System.err.println(get(key, args));
    }
}
