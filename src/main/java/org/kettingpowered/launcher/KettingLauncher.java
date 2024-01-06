package org.kettingpowered.launcher;

import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.ketting.internal.KettingFileVersioned;
import org.kettingpowered.ketting.internal.KettingFiles;
import org.kettingpowered.ketting.internal.MajorMinorPatchVersion;
import org.kettingpowered.ketting.internal.Tuple;
import org.kettingpowered.launcher.betterui.BetterUI;
import org.kettingpowered.launcher.dependency.AvailableMavenRepos;
import org.kettingpowered.launcher.dependency.Dependency;
import org.kettingpowered.launcher.dependency.LibHelper;
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
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
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

    private final Path eula = new File(KettingFiles.MAIN_FOLDER_FILE, "eula.txt").toPath();
    private final ParsedArgs args;
    private final BetterUI ui;
    private final Libraries libs = new Libraries();
    
    KettingLauncher(String[] str_args) throws Exception {
        ui = new BetterUI(KettingConstants.NAME);
        args = new Args(str_args).parse(ui, eula);
    }

    /**
     * Initialized stuff, so that KettingConstants and KettingFile are usable.
     * Also takes care of server updates.
     */
    public void init() throws Exception {
        final String mc_version = MCVersion.getMc(args);
        //This cannot get moved past the ensureOneServerAndUpdate call. 
        //It will cause the just downloaded server to be removed, which causes issues. 
        if (false) { //todo: patcher update checking
            if (Main.DEBUG) System.out.println("Patcher needs updating.");
            //prematurely delete files to prevent errors
            FileUtils.deleteDir(KettingFiles.NMS_BASE_DIR);
            FileUtils.deleteDir(KettingFiles.KETTINGSERVER_BASE_DIR);
        }

//        JavaHacks.setStreamFactory();
        ensureOneServerAndUpdate(mc_version);
//            JavaHacks.removeStreamFactory();
        
        ui.printTitle(mc_version);

        if(!ui.checkEula(eula)) System.exit(0);
        
        if (args.enableLauncherUpdator()) updateLauncher(); //it makes very little difference where this is. This will only be applied on the next reboot anyway.
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
        Tuple<MajorMinorPatchVersion<Integer>, MajorMinorPatchVersion<Integer>> serverVersion = null;
        
        if(versions.isEmpty()) FileUtils.deleteDir(KettingFiles.KETTINGSERVER_FORGE_DIR); //we have multiple ketting versions, but 0 that match the requested minecraft version.
        else if(versions.size() > 1) {
            serverVersion = versions.get(0);
            Tuple<MajorMinorPatchVersion<Integer>, MajorMinorPatchVersion<Integer>> version = serverVersion;
            Arrays.stream(Objects.requireNonNullElse(KettingFiles.KETTINGSERVER_FORGE_DIR.listFiles(File::isDirectory), new File[0]))
                    .filter(file -> !Objects.equals(file.getName(),String.format("%s-%s-%s", mc_mmp, version.t1(), version.t2())))
                    .forEach(FileUtils::deleteDir);
        }else{
            serverVersion = versions.get(0);
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
                System.err.println("Found no Ketting version for the requested Minecraft Version: " + mc_version);
                System.exit(1);
            }
            serverVersion = parsedServerVersions.get(0);
        }
        //todo: Server version is populated here. We could theoretically do what used to be the patched invalidation check here. 
        if (args.enableServerUpdator() || needsDownload) {
            final String mc_minecraft_forge = String.format("%s-%s-%s", mc_mmp, serverVersion.t1(), serverVersion.t2());
            final File forgeDir = new File(KettingFiles.KETTINGSERVER_FORGE_DIR,mc_minecraft_forge);
            
            //noinspection ResultOfMethodCallIgnored
            forgeDir.mkdirs();
            //noinspection ResultOfMethodCallIgnored
            KettingFiles.SERVER_LZMA.getParentFile().mkdirs();

            try{
                final MavenArtifact serverBinPatchesArtifact = new MavenArtifact(KettingConstants.KETTINGSERVER_GROUP, "forge", mc_minecraft_forge, Optional.of("server-bin-patches"), Optional.of("lzma"));
                final MavenArtifact installerJsonArtifact = new MavenArtifact(KettingConstants.KETTINGSERVER_GROUP, "forge", mc_minecraft_forge, Optional.of("installscript"), Optional.of("json"));
                final MavenArtifact kettingLibsArtifact = new MavenArtifact(KettingConstants.KETTINGSERVER_GROUP, "forge", mc_minecraft_forge, Optional.of("ketting-libraries"), Optional.of("txt"));
                final MavenArtifact universalJarArtifact = new MavenArtifact(KettingConstants.KETTINGSERVER_GROUP, "forge", mc_minecraft_forge, Optional.of("universal"), Optional.of("jar"));
                
                LibHelper.downloadDependency(LibHelper.downloadDependencyHash(serverBinPatchesArtifact));
                LibHelper.downloadDependency(LibHelper.downloadDependencyHash(installerJsonArtifact));
                LibHelper.downloadDependency(LibHelper.downloadDependencyHash(kettingLibsArtifact));
                LibHelper.downloadDependency(LibHelper.downloadDependencyHash(universalJarArtifact));
            }catch (IOException|NoSuchAlgorithmException ignored){
                FileUtils.deleteDir(forgeDir);
                throw ignored;
            }
        }
    }

    void launch() throws Exception {
        Libraries libs = new Libraries();
        {
            StringBuilder builder = new StringBuilder();
            try (BufferedReader stream = new BufferedReader(new FileReader(KettingFileVersioned.FORGE_KETTING_LIBS))) {
                libs.downloadExternal(
                        stream.lines()
                                .map(Dependency::parse)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .filter(dep->dep.maven().isPresent())
                                .filter(dep-> Arrays.stream(MANUALLY_PATCHED_LIBS).noneMatch(path -> dep.maven().get().getPath().startsWith(path)))
                                .peek(dep-> builder.append(File.pathSeparator).append(new File(KettingFiles.LIBRARIES_DIR, dep.maven().get().getPath()).getAbsolutePath()))
                                .toList(), 
                        true
                );
            }
            
            builder.append(File.pathSeparator).append(KettingFileVersioned.FORGE_UNIVERSAL_JAR.getAbsolutePath());
            MavenArtifact universalJarArtifact = new MavenArtifact(KettingConstants.KETTINGSERVER_GROUP, "forge", KettingConstants.MINECRAFT_VERSION+"-"+KettingConstants.FORGE_VERSION+"-"+KettingConstants.KETTING_VERSION, Optional.of("universal"), Optional.of("jar"));

            libs.loadDep(LibHelper.downloadDependencyHash(universalJarArtifact));
            
            System.setProperty("java.class.path", builder.toString());
            System.setProperty("ketting.remapper.dump", "./.mixin.out/plugin_classes");
            addToClassPath(KettingFileVersioned.FORGE_PATCHED_JAR);
            addToClassPath(KettingFileVersioned.SERVER_JAR);
//            addToClassPath(KettingFileVersioned.FORGE_UNIVERSAL_JAR);
//            addToClassPath(KettingFileVersioned.FMLCORE);
//            addToClassPath(KettingFileVersioned.FMLLOADER);
//            addToClassPath(KettingFileVersioned.JAVAFMLLANGUAGE);
//            addToClassPath(KettingFileVersioned.LOWCODELANGUAGE);
//            addToClassPath(KettingFileVersioned.MCLANGUAGE);
        }
        Libraries.downloadMcp();
        
        
        new Patcher(); //todo: patcher update checking 
        
        System.out.println("Launching Ketting...");
        final List<String> arg_list = new ArrayList<>(args.args());
        arg_list.add("--launchTarget");
        arg_list.add(args.launchTarget());

        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader loader = new LibraryClassLoader(libs.getLoadedLibs(), KettingLauncher.class.getClassLoader())) {
            Thread.currentThread().setContextClassLoader(loader);
            JavaHacks.loadExternalFileSystems(loader);
            JavaHacks.clearReservedIdentifiers();

            Class.forName("net.minecraftforge.bootstrap.ForgeBootstrap", true, loader)
                    .getMethod("main", String[].class)
                    .invoke(null, (Object) arg_list.toArray(String[]::new));
        }
//        catch (Throwable t) {
//            throw new RuntimeException("Could not launch server", t);
//        }
        finally{
            Thread.currentThread().setContextClassLoader(oldCL);
        }
    }

    private void addToClassPath(File file) {
        try {
            libs.addLoadedLib(file.toURI().toURL());//Yes, this is important, and fixes an issue with forge not finding language jars
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        System.setProperty("java.class.path", System.getProperty("java.class.path") + File.pathSeparator + file.getAbsolutePath());
    }
}
