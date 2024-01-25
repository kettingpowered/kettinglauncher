package org.kettingpowered.launcher;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kettingpowered.ketting.internal.*;
import org.kettingpowered.ketting.internal.hacks.ServerInitHelper;
import org.kettingpowered.launcher.betterui.BetterUI;
import org.kettingpowered.launcher.dependency.*;
import org.kettingpowered.launcher.info.MCVersion;
import org.kettingpowered.launcher.internal.utils.HashUtils;
import org.kettingpowered.launcher.lang.I18n;
import org.kettingpowered.launcher.utils.FileUtils;
import org.kettingpowered.ketting.internal.hacks.JavaHacks;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author C0D3 M4513R
 */
public class KettingLauncher {
    public static final String Version = (KettingLauncher.class.getPackage().getImplementationVersion() != null) ? KettingLauncher.class.getPackage().getImplementationVersion() : "dev-env";
    public static final boolean Bundled;
    @Nullable private static final String Bundled_McVersion;
    @Nullable private static final String Bundled_ForgeVersion;
    @Nullable private static final String Bundled_KettingVersion;

    static {
        String Bundled_KettingVersion1 = null;
        String Bundled_ForgeVersion1 = null;
        String Bundled_McVersion1 = null;
        boolean Bundled1 = false;

        
        try {
            for(final Enumeration<URL> url_enum = KettingLauncher.class.getClassLoader().getResources("META-INF/MANIFEST.MF"); url_enum.hasMoreElements(); ){
                try{
                    Attributes attr = new Manifest(url_enum.nextElement().openStream()).getMainAttributes();
                    if (!Main.class.getName().equals(attr.getValue("Main-Class")) || !Main.class.getName().equals(attr.getValue("Launcher-Agent-Class"))) continue;
                    Bundled1=Boolean.parseBoolean(attr.getValue("Bundled"));
                    if (!Bundled1) break;
                    Bundled_McVersion1 = attr.getValue("MinecraftVersion");
                    Bundled_ForgeVersion1 = attr.getValue("ForgeVersion");
                    Bundled_KettingVersion1 = attr.getValue("KettingVersion");
                } catch (IOException ignored){}
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Bundled_KettingVersion = Bundled_KettingVersion1;
        Bundled_ForgeVersion = Bundled_ForgeVersion1;
        Bundled_McVersion = Bundled_McVersion1;
        Bundled = Bundled1;
        if (Main.DEBUG && Bundled) I18n.log("debug.bundled.extra", Bundled_McVersion, Bundled_ForgeVersion, Bundled_KettingVersion);
    }

    public static final String ArtifactID = "kettinglauncher";
    @ApiStatus.Internal public static final int BufferSize = 1024*1024*32;

    //Libs added here will get ignored by lib (down-)loading and will also not get added to java.class.path  
    @ApiStatus.Internal public static final List<String> MANUALLY_PATCHED_LIBS = List.of(
            "com/mojang/brigadier"
    );

    @ApiStatus.Internal public static final List<MavenArtifact> AUTO_UPDATE_LIBS = List.of(
        Main.KETTINGCOMMON,
        new MavenArtifact(KettingConstants.KETTING_GROUP, "kettingcore", "1.0.0", Optional.empty(), Optional.of("jar")), 
        new MavenArtifact(KettingConstants.KETTING_GROUP, "terminal-colors", "1.0.0", Optional.empty(), Optional.of("jar"))
    );
    

    private final Path eula = new File(KettingFiles.MAIN_FOLDER_FILE, "eula.txt").toPath();
    final ParsedArgs args;
    private final BetterUI ui;
    final Libraries libs = new Libraries();

    private final List<String> serverVersions = new ArrayList<>();
    private final List<String> availableMcVersions = new ArrayList<>();
    
    KettingLauncher(String[] str_args) throws Exception {
        ui = new BetterUI(KettingConstants.NAME);
        args = new Args(str_args).parse(ui, eula);
    }

    /**
     * Initialized stuff, so that KettingConstants and KettingFile are usable.
     * Also takes care of server updates.
     */
    public void init() throws Exception {
        final String mc_version;
        if (Main.DEBUG && Bundled) I18n.log("debug.bundled");

        //Cache ketting versions
        final List<String> depVersions = new MavenManifest(KettingConstants.KETTINGSERVER_GROUP, Main.FORGE_SERVER_ARTIFACT_ID).getDepVersions();
        serverVersions.addAll(depVersions);

        //Strip the minecraft version from the ketting version, by splitting by - and getting the first entry
        availableMcVersions.addAll(depVersions.stream().map(version -> {
            try {
                return version.split("-")[0];
            } catch (Exception e) { //Make sure that we don't crash, if the version is invalid
                return null;
            }
        }).filter(Objects::nonNull).distinct().sorted().toList());

        if (!Bundled) mc_version = MCVersion.getMc(args, availableMcVersions);
        else mc_version = Bundled_McVersion;
        //This cannot get moved past the ensureOneServerAndUpdate call. 
        //It will cause the just downloaded server to be removed, which causes issues.

        if(!Bundled) ensureOneServerAndUpdate(mc_version);
        else extractBundledContent();

        ui.printTitle(mc_version);

        if (!Bundled && args.enableLauncherUpdator()) updateLauncher(); //it makes very little difference where this is. This will only be applied on the next reboot anyway.

        if (!args.installOnly())
            if(!ui.checkEula(eula)) System.exit(0);
    }

    private static void extractBundledContent() throws Exception {
        Exception exception = new Exception(I18n.get("error.launcher.bundle_extract_failed"));
        boolean failed = false;
        final String version = Bundled_McVersion+"-"+Bundled_ForgeVersion+"-"+Bundled_KettingVersion;
        deleteOtherVersions(version);
        for(final String prefix : new String[]{"fmlloader", "fmlcore", "javafmllanguage", "lowcodelanguage", "mclanguage"}){
            final String fileName = prefix+"-"+version + ".jar";
            final File file = new File(KettingFiles.KETTINGSERVER_BASE_DIR, String.format("%s/%s/%s", prefix, version, fileName));
            try {
                extractJarContent(KettingFiles.DATA_DIR+fileName, file);
            } catch (IOException e) {
                failed=true;
                exception.addSuppressed(e);
            }
        }
        final String fileName = Main.FORGE_SERVER_ARTIFACT_ID+"-"+version;
        final File folder = new File(KettingFiles.KETTINGSERVER_FORGE_DIR, version);
        try{
            extractJarContent(KettingFiles.DATA_DIR+"ketting_libraries.txt", new File(folder, fileName+"-ketting-libraries.txt"));
        } catch (IOException e) {
            failed=true;
            exception.addSuppressed(e);
        }
        try{
            extractJarContent(KettingFiles.DATA_DIR+fileName+"-universal.jar", new File(folder, fileName+"-universal.jar"));
        } catch (IOException e) {
            failed=true;
            exception.addSuppressed(e);
        }
        try{
            extractJarContent(KettingFiles.DATA_DIR+fileName+"-installscript.json", new File(folder, fileName+"-installscript.json"));
        } catch (IOException e) {
            failed=true;
            exception.addSuppressed(e);
        }
        try{
            extractJarContent(KettingFiles.DATA_DIR+"server.lzma", KettingFiles.SERVER_LZMA);
        } catch (IOException e) {
            failed=true;
            exception.addSuppressed(e);
        }
        if (failed) throw exception;
    }

    public static void extractJarContent(@NotNull String from, @NotNull File to) throws IOException {
        try (InputStream internalFile = KettingLauncher.class.getClassLoader().getResourceAsStream(from)) {
            if (internalFile == null)
                throw new IOException(I18n.get("error.launcher.bundled_file_not_found", from));

            byte[] internalFileContent = internalFile.readAllBytes();
            if (!to.exists() || !HashUtils.getHash(to, "SHA3-512").equals(HashUtils.getHash(new ByteArrayInputStream(internalFileContent), "SHA3-512"))) {
                //noinspection ResultOfMethodCallIgnored
                to.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                to.createNewFile();

                try (FileOutputStream fos = new FileOutputStream(to)) {
                    fos.write(internalFileContent);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(I18n.get("error.launcher.hash_algorithm_not_found"), e);
        }
    }

    private void updateLauncher() throws Exception {
        if ("dev-env".equals(Version)) return;
        //get a list of available launcher versions
        final List<MajorMinorPatchVersion<Integer>> launcherVersions = new MavenManifest(KettingConstants.KETTING_GROUP, ArtifactID).getDepVersions()
                .stream()
                .map(MajorMinorPatchVersion::parse)
                .map(mmp->mmp.convertMMP(Integer::parseInt))
                .sorted()
                .toList();
        if(Main.DEBUG) System.out.println(launcherVersions.stream().map(MajorMinorPatchVersion::toString).collect(Collectors.joining("\n")));
        MajorMinorPatchVersion<Integer> version = MajorMinorPatchVersion.parse(Version).convertMMP(Integer::parseInt);
        final int index = launcherVersions.indexOf(version);
        if (index<0) I18n.logError("error.launcher.unrecognized_version");
        //if we are the latest version, then no need to do anything
        else if (index==launcherVersions.size()-1) {
            I18n.log("info.launcher.up_to_date");
            return;
        }
        
        //else update the launcher jar. We cannot (and shouldn't) apply the update. It will be done on the next server boot.
        final MavenArtifact dep = new MavenArtifact(KettingConstants.KETTING_GROUP, ArtifactID, launcherVersions.get(launcherVersions.size()-1).toString(), Optional.empty(), Optional.of("jar"));

        if (dep.download().renameTo(Main.LauncherJar)) I18n.log("info.launcher.updated");
        else I18n.logError("error.launcher.update_failed");
    }
    private static void deleteOtherVersions(final String version){
        Arrays.stream(Objects.requireNonNullElse(KettingFiles.KETTINGSERVER_FORGE_DIR.listFiles(File::isDirectory), new File[0]))
                .filter(file -> !Objects.equals(file.getName(),version))
                .forEach(FileUtils::deleteDir);
    }
    
    private void ensureOneServerAndUpdate(final String mc_version) {
        //Parse the given minecraft version 
        final MajorMinorPatchVersion<Integer> mc_mmp;
        {
            MajorMinorPatchVersion<String> mc_mmp_str = MajorMinorPatchVersion.parse(mc_version);
            mc_mmp = new MajorMinorPatchVersion<>(Integer.parseInt(mc_mmp_str.major()), Integer.parseInt(mc_mmp_str.minor()), Integer.parseInt(mc_mmp_str.patch()), mc_mmp_str.other());
        }
        Tuple<MajorMinorPatchVersion<Integer>, MajorMinorPatchVersion<Integer>> serverVersion = null;
        final boolean needsDownload;
        //Get the latest Ketting server version for the minecraft version that exists in the KETTINGSERVER_FORGE_DIR and delete the rest. 
        {
            File[] kettingServerVersions = Objects.requireNonNullElse(KettingFiles.KETTINGSERVER_FORGE_DIR.listFiles(File::isDirectory), new File[0]);
    
            if (Main.DEBUG) {
                System.out.println(KettingFiles.KETTINGSERVER_FORGE_DIR.getAbsolutePath());
                System.out.println(Arrays.stream(kettingServerVersions).map(File::getName).collect(Collectors.joining("\n")));
            }
            final List<Tuple<MajorMinorPatchVersion<Integer>, MajorMinorPatchVersion<Integer>>> versions = MajorMinorPatchVersion.parseKettingServerVersionList(Arrays.stream(kettingServerVersions).map(File::getName)).getOrDefault(mc_mmp, new ArrayList<>());
            
            needsDownload = versions.isEmpty();
            if(needsDownload) FileUtils.deleteDir(KettingFiles.KETTINGSERVER_FORGE_DIR); //we have multiple ketting versions, but 0 that match the requested minecraft version.
            else if(versions.size() > 1) {
                serverVersion = versions.get(0);
                Tuple<MajorMinorPatchVersion<Integer>, MajorMinorPatchVersion<Integer>> version = serverVersion;
                deleteOtherVersions(String.format("%s-%s-%s", mc_mmp, version.t1(), version.t2()));
            }else{
                serverVersion = versions.get(0);
            }
        }
        
        //if we don't have a ketting server version for the given minecraft version or there is a new ketting server version for the given minecraft version and server updates are enabled:
        // download the newest version and launch that. 
        if (needsDownload) I18n.log("info.launcher.server_download", mc_version);
        // get the newest version
        if (args.enableServerUpdator() || needsDownload) {
            final List<Tuple<MajorMinorPatchVersion<Integer>, MajorMinorPatchVersion<Integer>>> parsedServerVersions = MajorMinorPatchVersion.parseKettingServerVersionList(serverVersions.stream()).getOrDefault(mc_mmp, new ArrayList<>());
            if (Main.DEBUG) {
                I18n.log("debug.launcher.available_server_versions");
                System.out.println(String.join("\n", serverVersions));
            }
            if (parsedServerVersions.isEmpty()) {
                I18n.logError("error.launcher.no_server_version", mc_version);
                System.exit(1);
            }
            serverVersion = parsedServerVersions.get(0);
        }
        if (Patcher.checkUpdateNeeded(mc_mmp.toString(), serverVersion.t1().toString(), serverVersion.t2().toString(), args.enableServerUpdator() || needsDownload)){
            if (Main.DEBUG) I18n.log("debug.patcher.update");
            //prematurely delete files to prevent errors
            FileUtils.deleteDir(KettingFiles.NMS_BASE_DIR);
            FileUtils.deleteDir(KettingFiles.KETTINGSERVER_BASE_DIR);
        }
        //and download it
        if (args.enableServerUpdator() || needsDownload) {
            final String mc_minecraft_forge = String.format("%s-%s-%s", mc_mmp, serverVersion.t1(), serverVersion.t2());
            final File forgeDir = new File(KettingFiles.KETTINGSERVER_FORGE_DIR,mc_minecraft_forge);
            
            //noinspection ResultOfMethodCallIgnored
            forgeDir.mkdirs();
            //noinspection ResultOfMethodCallIgnored
            KettingFiles.SERVER_LZMA.getParentFile().mkdirs();

            try{
                final MavenArtifact serverBinPatchesArtifact = new MavenArtifact(KettingConstants.KETTINGSERVER_GROUP, Main.FORGE_SERVER_ARTIFACT_ID, mc_minecraft_forge, Optional.of("server-bin-patches"), Optional.of("lzma"));
                final MavenArtifact installerJsonArtifact = new MavenArtifact(KettingConstants.KETTINGSERVER_GROUP, Main.FORGE_SERVER_ARTIFACT_ID, mc_minecraft_forge, Optional.of("installscript"), Optional.of("json"));
                final MavenArtifact kettingLibsArtifact = new MavenArtifact(KettingConstants.KETTINGSERVER_GROUP, Main.FORGE_SERVER_ARTIFACT_ID, mc_minecraft_forge, Optional.of("ketting-libraries"), Optional.of("txt"));
                final MavenArtifact universalJarArtifact = new MavenArtifact(KettingConstants.KETTINGSERVER_GROUP, Main.FORGE_SERVER_ARTIFACT_ID, mc_minecraft_forge, Optional.of("universal"), Optional.of("jar"));

                //noinspection ResultOfMethodCallIgnored
                KettingFiles.SERVER_LZMA.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                KettingFiles.SERVER_LZMA.delete();
                Stream.of(serverBinPatchesArtifact, installerJsonArtifact, kettingLibsArtifact, universalJarArtifact)
                        .parallel()
                        .forEach(element->{
                            try{
                                if (element == serverBinPatchesArtifact && !serverBinPatchesArtifact.download().renameTo(KettingFiles.SERVER_LZMA))
                                    I18n.logError("error.launcher.bin_patches_move");
                                else element.download();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
            }catch (Throwable exception){
                FileUtils.deleteDir(forgeDir);
                throw exception;
            }
        }
    }

    void prepareLaunch() throws Exception {
        Thread downloadMCP = new Thread(()-> {
            try {
                Libraries.downloadMcp();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        downloadMCP.setDaemon(true);
        downloadMCP.setName("Download-MCP");
        downloadMCP.start();
        {
            StringBuilder builder = new StringBuilder();
            try (BufferedReader stream = new BufferedReader(new FileReader(KettingFileVersioned.FORGE_KETTING_LIBS))) {
                libs.downloadExternal(
                        stream.lines()
                                .map(Dependency::parse)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .filter(dep-> MANUALLY_PATCHED_LIBS.stream().noneMatch(path -> dep.getPath().startsWith(path)))
                                .toList(), 
                        true
                );
            }
            
            System.setProperty("ketting.remapper.dump", "./.mixin.out/plugin_classes");
        }
        downloadMCP.join();
        if (Patcher.checkUpdateNeeded(KettingConstants.MINECRAFT_VERSION, KettingConstants.FORGE_VERSION, KettingConstants.KETTING_VERSION, false))
            new Patcher();

        JavaHacks.clearReservedIdentifiers();
        Arrays.stream(libs.getLoadedLibs())
                .forEach(url -> {
                        try {
                            Main.INST.appendToSystemClassLoaderSearch(new JarFile(new File(url.toURI())));
                        } catch (URISyntaxException | IOException e) {
                            throw new RuntimeException(e);
                        }
                });
        
        if (args.installOnly()) {
            I18n.log("info.launcher.install_only.success");
            System.exit(0);
        }
    }

    String findLaunchClass() throws ClassNotFoundException {
        ClassNotFoundException exception = new ClassNotFoundException(I18n.get("error.launcher.no_launch_class"));
        //the AgentClassLoader is used here, because it doesn't propagate to anywhere else.
        //if we were, we would get errors, that some package is already defined, because we loaded it here.
        ClassLoader cl = new AgentClassLoader(libs.getLoadedLibs());
        
        try {
            Class.forName("net.minecraftforge.bootstrap.ForgeBootstrap", true, cl);
            return "net.minecraftforge.bootstrap.ForgeBootstrap";
        }catch(Throwable t){
            exception.addSuppressed(t);
        }
        
        MajorMinorPatchVersion<Integer> mc = MajorMinorPatchVersion.parse(KettingConstants.MINECRAFT_VERSION).convertMMP(Integer::parseInt);
        
        if (mc.compareTo(new MajorMinorPatchVersion<>(1,20,1, null)) <=0 ){
            try {
                Class.forName("cpw.mods.bootstraplauncher.BootstrapLauncher", true, cl);
                return "cpw.mods.bootstraplauncher.BootstrapLauncher";
            }catch(Throwable t){
                exception.addSuppressed(t);
            }
        }

        throw exception;
    }
}
