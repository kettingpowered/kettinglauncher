package org.kettingpowered.launcher.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class I18n {

    private static final String LANG_PATH = "lang/";

    private static final Map<String, String> translations = new HashMap<>();
    private static final Map<String, String> fallback = new HashMap<>();

    private static Locale current;

    public static void load() {
        load(false);
    }

    public static void load(boolean silent) {
        if (current != null) return;

        current = Locale.getDefault();

        String lang = (current.getLanguage() + "_" + current.getCountry()).toLowerCase();
        loadFile(lang, translations, silent);

        //Load fallback translations
        if (!lang.equals("en_us"))
            loadFile("en_us", fallback, silent);

        if (!silent)
            System.out.println("Loaded " + translations.size() + " translations");
    }

    private static void loadFile(String langCode, Map<String, String> toAdd, boolean silent) {
        try (InputStream lang = I18n.class.getClassLoader().getResourceAsStream(LANG_PATH + langCode + ".json")) {
            if (lang == null && !silent) {
                System.out.println("Language file not found for " + langCode + ", using default");
                return;
            }

            final JsonElement json = JsonParser.parseReader(new InputStreamReader(lang));
            json.getAsJsonObject().entrySet().forEach(entry -> toAdd.put(entry.getKey(), entry.getValue().getAsString()));
        } catch (IOException io) {
            if (silent) return;
            System.err.println("Failed to load language file for " + langCode);
            io.printStackTrace();
        }
    }

    public static String get(@NotNull String key) {
        String translation = translations.get(key);
        if (translation == null) {
            System.err.println("Missing translation for " + key);

            if (fallback.isEmpty())
                return key;

            translation = fallback.get(key);
            if (translation == null) {
                System.err.println("Missing fallback translation for " + key);
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
