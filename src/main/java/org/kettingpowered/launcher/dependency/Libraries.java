package org.kettingpowered.launcher.dependency;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.launcher.KettingLauncher;
import org.kettingpowered.launcher.Main;
import org.kettingpowered.launcher.betterui.BetterUI;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * @author C0D3 M4513R
 */
public class Libraries {
    private final List<URL> loadedLibs = new ArrayList<>();
    
    public Libraries() {}
    public void downloadExternal(List<Dependency> dependencies, boolean progressBar) {
        Stream<Dependency> dependencyStream;
        AtomicReference<ProgressBar> progressBarAtomicReference = new AtomicReference<>();
        if (progressBar) {
            ProgressBarBuilder builder = new ProgressBarBuilder()
                    .setTaskName("Loading libs...")
                    .hideEta()
                    .setMaxRenderedLength(BetterUI.LOGO_LENGTH)
                    .setStyle(ProgressBarStyle.ASCII)
                    .setUpdateIntervalMillis(100)
                    .setInitialMax(dependencies.size()); 
            progressBarAtomicReference.set(builder.build());
            dependencyStream = ProgressBar.wrap(dependencies.stream(), builder);
        } else dependencyStream = dependencies.stream();
        
        dependencyStream.parallel()
                .map(this::downloadDep)
                .forEach(file->{
                    synchronized (loadedLibs){
                        try {
                            addLoadedLib(file);
                        }catch (MalformedURLException e){
                            throw new RuntimeException("Something went wrong whilst trying to load dependencies", e);
                        }
                        if (progressBar) progressBarAtomicReference.get().step();
                    }
                });
        if (progressBar) progressBarAtomicReference.get().close();
    }
    
    private File downloadDep(Dependency dep) {
        if (dep.maven().isEmpty()) throw new IllegalStateException("Loading a Dependency with no maven coordinates is unsupported.");
        if (KettingLauncher.Bundled && KettingConstants.KETTINGSERVER_GROUP.equals(dep.maven().get().group())) {
            if (Main.DEBUG) System.out.println("Skipping download of "+dep+", since it should be bundled.");
            return dep.maven().get().getDependencyPath().toFile().getAbsoluteFile();
        }
        File depFile;
        try{
            if (KettingLauncher.AUTO_UPDATE_LIBS.stream().anyMatch(artifact -> dep.maven().get().equalsIgnoringVersion(artifact))) {
                MavenArtifact artifact = dep.maven().get().getLatestMinorPatch();
                if (Main.DEBUG) System.out.println("Using "+artifact+" instead of "+dep.maven().get());
                if (!artifact.equals(dep.maven().get())) depFile = artifact.downloadDependencyAndHash();
                else depFile = dep.downloadDependency();
            }else {
                depFile = dep.downloadDependency();
            }
            return depFile;
        }catch (Exception e){
            throw new RuntimeException("Something went wrong whilst trying to load dependencies", e);
        }
    }

    public static void downloadMcp() throws Exception {
        String mcMcp = KettingConstants.MINECRAFT_VERSION + "-" + KettingConstants.MCP_VERSION;
        MavenArtifact mcp_artifact = new MavenArtifact("de.oceanlabs.mcp", "mcp_config", mcMcp, Optional.empty(), Optional.of("zip"));
        mcp_artifact.downloadDependencyAndHash();
    }

    public void addLoadedLib(File file) throws MalformedURLException {
        addLoadedLib(file.getAbsoluteFile().toPath());
    }
    public void addLoadedLib(Path path) throws MalformedURLException {
        addLoadedLib(path.toAbsolutePath().toUri());
    }
    public void addLoadedLib(URI uri) throws MalformedURLException {
        addLoadedLib(uri.toURL());
    }
    public void addLoadedLib(URL url){
        if (url == null) return;
        if (!loadedLibs.contains(url))
            loadedLibs.add(url);
    }
    
    public URL[] getLoadedLibs() {
        if (loadedLibs.stream().anyMatch(Objects::isNull)) {
            System.err.println("Failed to load libraries, please try again");
            System.exit(1);
        }

        return loadedLibs.toArray(new URL[0]);
    }
}
