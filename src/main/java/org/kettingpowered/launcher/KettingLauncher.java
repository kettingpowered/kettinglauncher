package org.kettingpowered.launcher;

import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.launcher.betterui.BetterUI;
import org.kettingpowered.launcher.dependency.AvailableMavenRepos;
import org.kettingpowered.launcher.dependency.Dependency;
import org.kettingpowered.launcher.dependency.Libraries;
import org.kettingpowered.launcher.dependency.LibraryClassLoader;
import org.kettingpowered.launcher.dependency.MavenArtifact;
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
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class KettingLauncher {
    public static final String Version = (KettingConstants.class.getPackage().getImplementationVersion() != null) ? KettingConstants.class.getPackage().getImplementationVersion() : "dev-env";
    public static final String ArtifactID = "kettinglauncher";
    public static final int BufferSize = 1024*1024*32;

    //Libs added here will get ignored by getClassPathFromShim
    public static final String[] MANUALLY_PATCHED_LIBS = {
            "com/mojang/brigadier",
    };
    
    private final ParsedArgs args;
    private final Libraries libs = new Libraries();

    public static void main(String[] args) throws Exception {
        //These shall be strictly Launcher libs.
        //No wierd classpath shenanigans necessary, because the Class-Path is set in the Manifest.
        //Even if those libs don't exist in the FileSystem yet
        final Libraries libs = new Libraries();
        //Download all needed libs for the Launcher itself
        try (BufferedReader stream = new BufferedReader(new InputStreamReader(Objects.requireNonNull(KettingLauncher.class.getClassLoader().getResourceAsStream("data/launcher_libraries.txt"))))){
            libs.downloadExternal(stream.lines().map(Dependency::parse).filter(Optional::isPresent).map(Optional::get).toList(), false);
        }
        
        final KettingLauncher launcher = new KettingLauncher(args);
        launcher.launch();
    }
    
    private KettingLauncher(String[] str_args) throws Exception {
        Path eula = Paths.get("eula.txt");
        BetterUI ui = new BetterUI(KettingConstants.NAME);
        args = new Args(str_args).parse(ui, eula);
        
        ui.printTitle();
        
        if (args.enableServerUpdator() && !"dev-env".equals(Version)){
            final List<String> launcherVersions = MavenArtifact.getDepVersions(KettingConstants.KETTING_GROUP, ArtifactID);
            final int index = launcherVersions.indexOf(Version);
            if (index<0) System.err.println("Using unrecognised Launcher version.");
            else if (index>0){
                System.err.println("Using old Launcher version.");
                final String newVersion = launcherVersions.get(0);
                String path = new MavenArtifact(KettingConstants.KETTING_GROUP, ArtifactID, newVersion, Optional.empty(), Optional.of("jar")).getPath();
                boolean downloaded = false;
                Exception exception = new Exception("Failed to download new Launcher version '"+newVersion+"' from all repositories.");
                for(final String repo: AvailableMavenRepos.INSTANCE){
                    try{
                        NetworkUtils.downloadFile(repo + path, JarTool.getJar(), NetworkUtils.downloadToString(repo+path+".sha512", null, null), "SHA-512");
                        downloaded = true;
                    }catch (Throwable throwable){
                        exception.addSuppressed(throwable);
                    }
                }
                if(!downloaded) throw exception;
                System.err.println("Downloaded a Launcher update. Please restart!");
            }
        }
        
        if(!ui.checkEula(eula)) System.exit(0);
        
        if (Patcher.updateNeeded()) {
            //prematurely delete files to prevent errors
            FileUtils.deleteDir(KettingFiles.NMS_PATCHES_DIR);
            FileUtils.deleteDir(KettingFiles.KETTINGSERVER_BASE_DIR);
        }
        if (args.enableServerUpdator()) {
            JavaHacks.setStreamFactory();
            UpdateChecker.tryUpdate();
            JavaHacks.removeStreamFactory();
        }
    }
    
    @SuppressWarnings("unused")
    public void lib_init() throws IOException, NoSuchAlgorithmException {
        new Patcher();
    }

    private void launch() {
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
        addToClassPath(KettingFiles.FORGE_PATCHED_JAR);
        addToClassPath(KettingFiles.FMLCORE);
        addToClassPath(KettingFiles.FMLLOADER);
        addToClassPath(KettingFiles.JAVAFMLLANGUAGE);
        addToClassPath(KettingFiles.LOWCODELANGUAGE);
        addToClassPath(KettingFiles.MCLANGUAGE);
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
