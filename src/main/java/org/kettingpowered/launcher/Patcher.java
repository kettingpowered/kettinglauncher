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
import org.kettingpowered.launcher.dependency.*;
import org.kettingpowered.launcher.internal.utils.HashUtils;
import org.kettingpowered.launcher.internal.utils.NetworkUtils;
import org.kettingpowered.launcher.lang.I18n;
import org.kettingpowered.launcher.utils.Processors;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Patcher {

    private final List<JsonObject> processors = new ArrayList<>();
    private final Map<String, String> tokens = new HashMap<>();

    private final PrintStream out = System.out;
    private PrintStream log;
    Libraries libraries;

    public Patcher(Libraries libraries) throws IOException, NoSuchAlgorithmException, ClassNotFoundException, NoSuchMethodException {
        this.libraries = libraries;
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
            I18n.logError("error.patcher.no_version_manifest");
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
            I18n.logError("error.patcher.no_release", KettingConstants.MINECRAFT_VERSION);
            System.exit(1);
        }

        final String releaseManifest = NetworkUtils.readFile(currentRelease.get("url").getAsString());
        if (releaseManifest == null) {
            I18n.logError("error.patcher.no_release_manifest");
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
            throw new IOException(I18n.get("error.patcher.failed_download"), e);
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
            I18n.logError("error.patcher.failed_read_install_script");
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
        tokens.put("{MC_OFF}", KettingFileVersioned.SERVER_SRG.getAbsolutePath()); //Basically the same as the one above
        tokens.put("{PATCHED}", KettingFileVersioned.FORGE_PATCHED_JAR.getAbsolutePath());
        tokens.put("{BINPATCH}", KettingFiles.SERVER_LZMA.getAbsolutePath());
    }

    private void readAndExecuteProcessors() throws IOException, NoSuchAlgorithmException, ClassNotFoundException, NoSuchMethodException {
        //we wrap this, for a similar reason as in KettingLauncher#findMainClass:
        //if we didn't, stuff that would get loaded into the system classloader by this would get flagged as being part of our module, when it shouldn't be. 
        AgentClassLoader cl = new AgentClassLoader(libraries.getLoadedLibs());

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

                //We also set the ContextClassLoader here, just in case a Processor does something stupid.
                ClassLoader ocl = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(cl);
                    mute();
                    Processors.execute(cl,  jar, parsedArgs.toArray(String[]::new));
                    unmute();
                    progressBar.step();
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    unmute();
                    throw new RuntimeException(I18n.get("error.patcher.processor_fault"), e);
                } finally{
                    Thread.currentThread().setContextClassLoader(ocl);
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

    public static boolean checkUpdateNeeded(String mcVersion, String forgeVersion, String kettingVersion, boolean updating) {
        if (!KettingFiles.STORED_HASHES.exists()) return true;
        try (BufferedReader reader = new BufferedReader(new FileReader(KettingFiles.STORED_HASHES))){
            return reader.lines()
                    .anyMatch(string -> {
                        String[] args = string.split("=");
                        String value = args[1].trim();
                        try {
                            return switch (args[0].trim()) {
                                case "installJson" ->
                                        checkUpdateNeededInstallerJson(mcVersion, forgeVersion, kettingVersion, value, updating);
                                case "serverLzma" ->
                                        !value.equals(HashUtils.getHash(KettingFiles.SERVER_LZMA, "SHA3-512"));
                                case "server" -> checkUpdateNeededServer(mcVersion, value);
                                default -> false;
                            };
                        } catch (Exception e) {
                            return false;
                        }
                    });
        }catch (Exception ignored) {
            return true;
        }
    }
    public static boolean checkUpdateNeededServer(String mcVersion, String hash) throws Exception {
        final File NMSDir = new File(KettingFiles.NMS_BASE_DIR, mcVersion);
        final File SERVER_JAR = new File(NMSDir, "server-" + mcVersion + ".jar");
        boolean upToDate = hash.equals(HashUtils.getHash(SERVER_JAR, "SHA3-512"));

        if (upToDate) I18n.logDebug("debug.patcher.upToDate.server");
        else I18n.logDebug("debug.patcher.notUpToDate.server");

        return !upToDate;
    }
    public static boolean checkUpdateNeededInstallerJson(String mcVersion, String forgeVersion, String kettingVersion, String hash, boolean updating) throws Exception {
        final String mcForgeKettingVersion = mcVersion+"-"+forgeVersion+"-"+kettingVersion;
        if (updating){
            Dependency<MavenArtifact> dep = new MavenArtifact(
                    KettingConstants.KETTINGSERVER_GROUP,
                    Main.FORGE_SERVER_ARTIFACT_ID,
                    mcForgeKettingVersion,
                    Optional.of("installscript"),
                    Optional.of("json")
            ).downloadDependencyHash();
            if (Maven.needsDownload(dep.hash(), KettingFiles.LIBRARIES_DIR.toPath(), dep)) return false;
        }
        final File forgeServerDir = new File(KettingFiles.KETTINGSERVER_FORGE_DIR, mcForgeKettingVersion);
        final File installJson = new File(forgeServerDir, "forge-" + mcForgeKettingVersion + "-installscript.json");
        boolean upToDate = hash.equals(HashUtils.getHash(installJson,"SHA3-512"));

        if (upToDate) I18n.logDebug("debug.patcher.upToDate.installerJson");
        else I18n.logDebug("debug.patcher.notUpToDate.installerJson");

        return !upToDate;
    }


    private void mute() {
        System.setOut(log);
    }

    private void unmute() {
        System.setOut(out);
    }
    
}
