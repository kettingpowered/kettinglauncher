package org.kettingpowered.launcher;

import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.ketting.internal.KettingFileVersioned;
import org.kettingpowered.ketting.internal.KettingFiles;
import org.kettingpowered.ketting.internal.MajorMinorPatchVersion;
import org.kettingpowered.ketting.internal.Tuple;
import org.kettingpowered.launcher.betterui.BetterUI;
import org.kettingpowered.launcher.dependency.AvailableMavenRepos;
import org.kettingpowered.launcher.dependency.Libraries;
import org.kettingpowered.launcher.dependency.LibraryClassLoader;
import org.kettingpowered.launcher.dependency.MavenArtifact;
import org.kettingpowered.launcher.info.MCVersion;
import org.kettingpowered.launcher.internal.utils.JarTool;
import org.kettingpowered.launcher.internal.utils.NetworkUtils;
import org.kettingpowered.launcher.utils.FileUtils;
import org.kettingpowered.launcher.utils.JavaHacks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.kettingpowered.ketting.internal.MajorMinorPatchVersion.parseKettingServerVersionList;

/**
 * @author C0D3 M4513R
 */
public class KettingLauncher {
    public static final String Version = (KettingLauncher.class.getPackage().getImplementationVersion() != null) ? KettingLauncher.class.getPackage().getImplementationVersion() : "dev-env";
    public static final String ArtifactID = "kettinglauncher";
    public static final int BufferSize = 1024*1024*32;

    //Libs added here will get ignored by getClassPathFromShim
    public static final String[] MANUALLY_PATCHED_LIBS = {
            "com/mojang/brigadier",
    };

    private final Path eula = Paths.get("eula.txt");
    private final ParsedArgs args;
    private final BetterUI ui;
    private final Libraries libs = new Libraries();
    
    KettingLauncher(String[] str_args) throws Exception {
        ui = new BetterUI(KettingConstants.NAME);
        args = new Args(str_args).parse(ui, eula);
    }
    
    public void init() throws Exception {
        final String mc_version = MCVersion.getMc(args);
//        JavaHacks.setStreamFactory();
        ensureOneServerAndUpdate(mc_version);
//            JavaHacks.removeStreamFactory();
        
        ui.printTitle(mc_version);

        if(!ui.checkEula(eula)) System.exit(0);
        
        if (args.enableLauncherUpdator()) updateLauncher(); //it makes very little difference where this is. This will only be applied on the next reboot anyway.
        if (Patcher.updateNeeded()) {
            //prematurely delete files to prevent errors
            FileUtils.deleteDir(KettingFileVersioned.NMS_PATCHES_DIR);
            FileUtils.deleteDir(KettingFiles.KETTINGSERVER_BASE_DIR);
        }
    }
    
    private void updateLauncher() throws Exception {
        final List<String> launcherVersions = MavenArtifact.getDepVersions(KettingConstants.KETTING_GROUP, ArtifactID);
        final int index = launcherVersions.indexOf(Version);
        if (index<0) {
            System.err.println("Using unrecognised Launcher version.");
            return;
        }
        else if (index==0) {
            System.out.println("Already on newest Launcher version. Nothing to do.");
            return;
        }

        final String newVersion = launcherVersions.get(0);
        String path = new MavenArtifact(KettingConstants.KETTING_GROUP, ArtifactID, newVersion, Optional.empty(), Optional.of("jar")).getPath();
        boolean downloaded = false;
        Exception exception = new Exception("Failed to download new Launcher version '"+newVersion+"' from all repositories.");
        for(final String repo: AvailableMavenRepos.INSTANCE){
            try{
                NetworkUtils.downloadFile(repo + path, JarTool.getJar(), NetworkUtils.readFile(repo+path+".sha512"), "SHA-512");
                downloaded = true;
            }catch (Throwable throwable){
                exception.addSuppressed(throwable);
            }
        }
        if(!downloaded) throw exception;
        System.err.println("Downloaded a Launcher update. A restart is required to apply the launcher update.");
    }
    
    private void ensureOneServerAndUpdate(final String mc_version) throws Exception {
        File[] kettingServerVersions = Objects.requireNonNullElse(KettingFiles.KETTINGSERVER_FORGE_DIR.listFiles(File::isDirectory), new File[0]);
        MajorMinorPatchVersion<Integer> mc_mmp;
        {
            MajorMinorPatchVersion<String> mc_mmp_str = MajorMinorPatchVersion.parse(mc_version);
            mc_mmp = new MajorMinorPatchVersion<>(Integer.parseInt(mc_mmp_str.major()), Integer.parseInt(mc_mmp_str.minor()), Integer.parseInt(mc_mmp_str.patch()));
        }

        if (Main.DEBUG) {
            System.out.println(KettingFiles.KETTINGSERVER_FORGE_DIR.getAbsolutePath());
            System.out.println(Arrays.stream(kettingServerVersions).map(File::getName).collect(Collectors.joining("\n")));
        }
        final List<Tuple<MajorMinorPatchVersion<Integer>, MajorMinorPatchVersion<Integer>>> versions = parseKettingServerVersionList(Arrays.stream(kettingServerVersions).map(File::getName)).getOrDefault(mc_mmp, new ArrayList<>());
        
        if(versions.isEmpty()) FileUtils.deleteDir(KettingFiles.KETTINGSERVER_FORGE_DIR); //we have multiple ketting versions, but 0 that match the requested minecraft version.
        else if(versions.size() > 1) {
            Tuple<MajorMinorPatchVersion<Integer>, MajorMinorPatchVersion<Integer>> version = versions.get(0);
            Arrays.stream(Objects.requireNonNullElse(KettingFiles.KETTINGSERVER_FORGE_DIR.listFiles(File::isDirectory), new File[0]))
                    .filter(file -> !Objects.equals(file.getName(),String.format("%s-%s-%s", mc_mmp, version.t1(), version.t2())))
                    .forEach(FileUtils::deleteDir);
        }
        final boolean needsDownload = versions.isEmpty();

        if (needsDownload) System.out.println("Downloading Server, since there is none currently present. Using determined Minecraft version: "+ mc_version);
        if (args.enableServerUpdator() || needsDownload) {
            final List<String> serverVersions = MavenArtifact.getDepVersions(KettingConstants.KETTINGSERVER_GROUP, "forge");
            final List<Tuple<MajorMinorPatchVersion<Integer>, MajorMinorPatchVersion<Integer>>> parsedServerVersions = parseKettingServerVersionList(serverVersions.stream()).getOrDefault(mc_mmp, new ArrayList<>());
            if (Main.DEBUG) {
                System.out.println("Available Ketting versions");
                System.out.println(String.join("\n", serverVersions));
            }
            if (parsedServerVersions.isEmpty()) {
                System.err.println("Found no Ketting version for the requested Minecraft Version: "+mc_version);
                System.exit(1);
            }
            Tuple<MajorMinorPatchVersion<Integer>, MajorMinorPatchVersion<Integer>> version = parsedServerVersions.get(0);
            final String mc_minecraft_forge = String.format("%s-%s-%s", mc_mmp, version.t1(), version.t2());
            final String path = MavenArtifact.getPath(KettingConstants.KETTINGSERVER_GROUP, Main.FORGE_SERVER_ARTIFACT_ID)+"/" + mc_minecraft_forge + "/";
            final File forgeDir = new File(KettingFiles.KETTINGSERVER_FORGE_DIR,mc_minecraft_forge);
            final String forgeFilePrefix = Main.FORGE_SERVER_ARTIFACT_ID+"-"+mc_minecraft_forge+"-";
            final String serverBinPatchesEnding = "server-bin-patches.lzma";
            final String installerJsonEnding = "installscript.json";
            final String kettingLibsEnding = "ketting-libraries.txt";
            final String universalJarEnding = "universal.jar";
            final File installerJson = new File(forgeDir, forgeFilePrefix+installerJsonEnding);
            final File kettingLibs = new File(forgeDir, forgeFilePrefix+kettingLibsEnding);
            final File universalJar = new File(forgeDir, forgeFilePrefix+universalJarEnding);
            
            //noinspection ResultOfMethodCallIgnored
            forgeDir.mkdirs();
            //noinspection ResultOfMethodCallIgnored
            KettingFiles.SERVER_LZMA.getParentFile().mkdirs();
            Exception exception = new Exception("Failed to download all required files");
            boolean downloaded = false;
            for (final String repo:AvailableMavenRepos.INSTANCE) {
                Exception exception1 = new Exception("the repo only provided some files");
                boolean allFine = true;
                try{
                    final String serverBinPatches = repo+path+forgeFilePrefix+serverBinPatchesEnding;
                    NetworkUtils.downloadFile(serverBinPatches, KettingFiles.SERVER_LZMA, NetworkUtils.readFile(serverBinPatches+".sha512"),"SHA-512");
                }catch (Throwable throwable) {
                    exception1.addSuppressed(throwable);
                    allFine = false;
                }
                
                try {
                    final String installerJsonURL = repo+path+forgeFilePrefix+installerJsonEnding;
                    NetworkUtils.downloadFile(installerJsonURL, installerJson, NetworkUtils.readFile(installerJsonURL+".sha512"), "SHA-512");
                }catch (Throwable throwable){
                    exception1.addSuppressed(throwable);
                    allFine = false;
                }
                
                try {
                    final String kettingLibsURL = repo+path+forgeFilePrefix+kettingLibsEnding;
                    NetworkUtils.downloadFile(kettingLibsURL, kettingLibs, NetworkUtils.readFile(kettingLibsURL+".sha512"), "SHA-512");
                } catch (Throwable throwable){
                    exception1.addSuppressed(throwable);
                    allFine = false;
                }

                try {
                    final String universalJarURL = repo+path+forgeFilePrefix+universalJarEnding;
                    NetworkUtils.downloadFile(universalJarURL, universalJar, NetworkUtils.readFile(universalJarURL+".sha512"), "SHA-512");
                } catch (Throwable throwable){
                    exception1.addSuppressed(throwable);
                    allFine = false;
                }
                
                if (allFine) {
                    downloaded = true;
                    break;
                }
                exception.addSuppressed(exception1);
            }
            if (!downloaded) {
                FileUtils.deleteDir(forgeDir);
                throw exception;
            }
        }
    }

    void launch() {
        System.out.println("Launching Ketting...");
        final List<String> arg_list = new ArrayList<>(args.args());
        arg_list.add("--launchTarget");
        arg_list.add(args.launchTarget());

        setProperties();
        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader loader = new LibraryClassLoader(libs.getLoadedLibs(), KettingLauncher.class.getClassLoader())) {
            Thread.currentThread().setContextClassLoader(loader);
            JavaHacks.loadExternalFileSystems(loader);
            JavaHacks.clearReservedIdentifiers();

            Class.forName("net.minecraftforge.bootstrap.ForgeBootstrap", true, loader)
                    .getMethod("main", String[].class)
                    .invoke(null, (Object) arg_list.toArray(String[]::new));
        } catch (Throwable t) {
            throw new RuntimeException("Could not launch server", t);
        } finally{
            Thread.currentThread().setContextClassLoader(oldCL);
        }
    }

    private void setProperties() {
        System.setProperty("java.class.path", getClassPathFromShim());
        addToClassPath(KettingFileVersioned.FORGE_PATCHED_JAR);
        addToClassPath(KettingFileVersioned.FMLCORE);
        addToClassPath(KettingFileVersioned.FMLLOADER);
        addToClassPath(KettingFileVersioned.JAVAFMLLANGUAGE);
        addToClassPath(KettingFileVersioned.LOWCODELANGUAGE);
        addToClassPath(KettingFileVersioned.MCLANGUAGE);
        System.setProperty("ketting.remapper.dump", "./.mixin.out/plugin_classes");
    }

    private void addToClassPath(File file) {
        try {
            libs.addLoadedLib(file.toURI().toURL());//Yes, this is important, and fixes an issue with forge not finding language jars
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        System.setProperty("java.class.path", System.getProperty("java.class.path") + File.pathSeparator + file.getAbsolutePath());
    }

    /**
     * @author JustRed23
     */
    private String getClassPathFromShim() {
        InputStream stream = KettingLauncher.class.getClassLoader().getResourceAsStream("data/bootstrap-shim.list");
        if (stream == null) throw new RuntimeException("Could not find bootstrap-shim.list");

        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            for(line = reader.readLine(); line != null; line = reader.readLine()) {
                String shim = line.split("\t")[2];
                for (String lib : MANUALLY_PATCHED_LIBS) {
                    if (shim.startsWith(lib)) {
                        shim = null;
                        break;
                    }
                }

                if (shim == null) continue;

                File target = new File(KettingFiles.LIBRARIES_PATH, shim);
                if (!target.exists()) {
                    System.err.println("Could not find " + shim + " in " + KettingConstants.INSTALLER_LIBRARIES_FOLDER);
                    continue;
                }

                builder.append(File.pathSeparator).append(target.getAbsolutePath());
                libs.addLoadedLib(target.toURI().toURL()); //Yes, this is important, and fixes an issue with forge not finding forge-universal jar
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read bootstrap-shim.list", e);
        }

        return builder.toString();
    }
}
