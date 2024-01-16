package org.kettingpowered.launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.ketting.internal.KettingFileVersioned;
import org.kettingpowered.ketting.internal.KettingFiles;
import org.kettingpowered.launcher.betterui.BetterUI;
import org.kettingpowered.launcher.internal.utils.HashUtils;
import org.kettingpowered.launcher.internal.utils.NetworkUtils;
import org.kettingpowered.launcher.utils.Processors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Patcher {

    private final List<JsonObject> processors = new ArrayList<>();
    private final Map<String, String> tokens = new HashMap<>();

    private final PrintStream out = System.out;
    private PrintStream log;

    public Patcher() throws IOException, NoSuchAlgorithmException {
        downloadServer();
        readInstallScript();
        prepareTokens();
        readAndExecuteProcessors();
    }

    private void downloadServer() throws IOException {
        final File serverJar = KettingFileVersioned.SERVER_JAR;
        if (serverJar.exists()) return;

        final String manifest = NetworkUtils.readFile("https://launchermeta.mojang.com/mc/game/version_manifest.json");
        if (manifest == null) {
            System.err.println("Failed to download version_manifest.json");
            System.exit(1);
        }

        final JsonArray releases = JsonParser.parseString(manifest).getAsJsonObject().getAsJsonArray("versions");

        JsonObject currentRelease = null;
        for (JsonElement release : releases) {
            JsonObject releaseObject = release.getAsJsonObject();
            if (releaseObject.get("id").getAsString().equals(KettingConstants.MINECRAFT_VERSION)) {
                currentRelease = releaseObject;
                break;
            }
        }

        if (currentRelease == null) {
            System.err.println("Failed to find release for version " + KettingConstants.MINECRAFT_VERSION);
            System.exit(1);
        }

        final String releaseManifest = NetworkUtils.readFile(currentRelease.get("url").getAsString());
        if (releaseManifest == null) {
            System.err.println("Failed to download release manifest");
            System.exit(1);
        }

        final JsonObject serverComponents = JsonParser.parseString(releaseManifest).getAsJsonObject()
                .getAsJsonObject("downloads").getAsJsonObject("server");
        final String serverUrl = serverComponents.get("url").getAsString();
        final String serverHash = serverComponents.get("sha1").getAsString();

        //noinspection ResultOfMethodCallIgnored
        serverJar.getParentFile().mkdirs();
        try {
            NetworkUtils.downloadFile(serverUrl, serverJar, serverHash, "sha1");
        } catch (Exception e) {
            throw new IOException("Failed to download server jar", e);
        }
    }

    private void readInstallScript() {
        try(FileReader reader = new FileReader(KettingFileVersioned.FORGE_INSTALL_JSON)){
            final JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray rawProcessors = object.getAsJsonArray("processors");
    
            rawProcessors.forEach(p -> {
                JsonObject processor = p.getAsJsonObject();
                if (!processor.has("sides") || processor.get("sides").getAsString().contains("server"))
                    processors.add(processor);
            });
    
            processors.remove(0); //Remove the extracting processor, we'll handle that ourselves
        }catch (IOException exception){
            System.err.println("Failed to load/read installscript.json");
            System.exit(1);
        }
    }
    

    private void prepareTokens() {
        tokens.put("{SIDE}", "server");
        tokens.put("{ROOT}/libraries/", KettingFiles.LIBRARIES_PATH); //Change the libraries folder to our custom one
        tokens.put("{MINECRAFT_JAR}", KettingFileVersioned.SERVER_JAR.getAbsolutePath());
        tokens.put("{MC_UNPACKED}", KettingFileVersioned.SERVER_UNPACKED.getAbsolutePath());
        tokens.put("{MAPPINGS}", KettingFileVersioned.MCP_MAPPINGS.getAbsolutePath());
        tokens.put("{MOJMAPS}", KettingFileVersioned.MOJANG_MAPPINGS.getAbsolutePath());
        tokens.put("{MERGED_MAPPINGS}", KettingFileVersioned.MERGED_MAPPINGS.getAbsolutePath());
        tokens.put("{MC_SLIM}", KettingFileVersioned.SERVER_SLIM.getAbsolutePath());
        tokens.put("{MC_EXTRA}", KettingFileVersioned.SERVER_EXTRA.getAbsolutePath());
        tokens.put("{MC_SRG}", KettingFileVersioned.SERVER_SRG.getAbsolutePath());
        tokens.put("{PATCHED}", KettingFileVersioned.FORGE_PATCHED_JAR.getAbsolutePath());
        tokens.put("{BINPATCH}", KettingFiles.SERVER_LZMA.getAbsolutePath());
    }

    private void readAndExecuteProcessors() throws IOException, NoSuchAlgorithmException {
        final File logFile = KettingFiles.PATCHER_LOGS;
        if (!logFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                logFile.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                logFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        log = new PrintStream(new FileOutputStream(KettingFiles.PATCHER_LOGS) {
            @Override
            public void write(int b) {
                out.write(b);
            }
        });

        try (ProgressBar progressBar = new ProgressBarBuilder()
                .setTaskName("Patching...")
                .hideEta()
                .setMaxRenderedLength(BetterUI.LOGO_LENGTH)
                .setStyle(ProgressBarStyle.ASCII)
                .setUpdateIntervalMillis(100)
                .setInitialMax(processors.size())
                .build())
        {
            processors.forEach(processorData -> {
                String jar = processorData.get("jar").getAsString();
                JsonArray args = processorData.getAsJsonArray("args");
                List<String> parsedArgs = new ArrayList<>();

                args.forEach(arg -> {
                    String argString = arg.getAsString();
                    if (argString.startsWith("[de.oceanlabs.mcp:mcp_config:")) {
                        argString = KettingFileVersioned.MCP_ZIP.getAbsolutePath();
                        parsedArgs.add(argString);
                        return;
                    }

                    for (Map.Entry<String, String> token : tokens.entrySet()) {
                        if (argString.equals(token.getKey())) {
                            argString = argString.replace(token.getKey(), token.getValue());
                            break;
                        }
                    }

                    parsedArgs.add(argString);
                });

                try {
                    mute();
                    Processors.execute(jar, parsedArgs.toArray(String[]::new));
                    unmute();
                    progressBar.step();
                } catch (IOException e) {
                    unmute();
                    throw new RuntimeException("A processor ran into an error", e);
                }
            });
        }
        writeStoredHashes();
    }
    private static void writeStoredHashes() throws IOException, NoSuchAlgorithmException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(KettingFiles.STORED_HASHES))){
            writer
                    .append("installJson=")
                    .append(HashUtils.getHash(KettingFileVersioned.FORGE_INSTALL_JSON, "SHA3-512"))
                    .append("\nserverLzma=")
                    .append(HashUtils.getHash(KettingFiles.SERVER_LZMA, "SHA3-512"))
                    .append("\nserver=")
                    .append(HashUtils.getHash(KettingFileVersioned.SERVER_JAR, "SHA3-512"));
        }
    }
    public static boolean checkUpdateNeeded() {
        if (!KettingFiles.STORED_HASHES.exists()) return true;
        try (BufferedReader reader = new BufferedReader(new FileReader(KettingFiles.STORED_HASHES))){
            return !reader.lines()
                    .allMatch(string -> {
                        String[] args = string.split("=");
                        String value = args[1].trim();
                        try {
                            return switch (args[0].trim()) {
                                case "installJson" ->
                                        value.equals(HashUtils.getHash(KettingFileVersioned.FORGE_INSTALL_JSON, "SHA3-512"));
                                case "serverLzma" ->
                                        value.equals(HashUtils.getHash(KettingFiles.SERVER_LZMA, "SHA3-512"));
                                case "server" ->
                                    value.equals(HashUtils.getHash(KettingFileVersioned.SERVER_JAR, "SHA3-512"));
                                default -> false;
                            };
                        } catch (NoSuchAlgorithmException | IOException e) {
                            return false;
                        }
                    });
        }catch (Exception ignored) {
            return true;
        }
    }

    private void mute() {
        System.setOut(log);
    }

    private void unmute() {
        System.setOut(out);
    }
    
}
