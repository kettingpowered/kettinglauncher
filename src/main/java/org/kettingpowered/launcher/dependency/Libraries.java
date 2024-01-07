package org.kettingpowered.launcher.dependency;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.launcher.KettingLauncher;
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
import java.util.stream.Stream;

/**
 * @author C0D3 M4513R
 */
public class Libraries {
    private final List<URL> loadedLibs = new ArrayList<>();
    
    public Libraries() {}
    public void downloadExternal(List<Dependency> dependencies, boolean progressBar) {
        Stream<Dependency> dependencyStream;
        if (progressBar) {
            ProgressBarBuilder builder = new ProgressBarBuilder()
                    .setTaskName("Loading libs...")
                    .hideEta()
                    .setMaxRenderedLength(BetterUI.LOGO_LENGTH)
                    .setStyle(ProgressBarStyle.ASCII)
                    .setUpdateIntervalMillis(100)
                    .setInitialMax(dependencies.size());
            
            dependencyStream = ProgressBar.wrap(dependencies.stream(), builder);
        } else dependencyStream = dependencies.stream();
        
        dependencyStream.forEach(this::loadDep);
    }
    
    public void loadDep(Dependency dep) {
        try{
            if (dep.maven().isPresent() && KettingLauncher.AUTO_UPDATE_LIBS.stream().anyMatch(artifact -> dep.maven().get().equalsIgnoringVersion(artifact))) {
                MavenArtifact artifact = dep.maven().get().getLatestMinorPatch();
                if (artifact!=dep.maven().get()) artifact.downloadDependencyAndHash();
                else dep.downloadDependency();
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        //AUTO_UPDATE_LIBS end
        if (dep.maven().isEmpty()) throw new IllegalStateException("Loading a Dependency with no maven coordinates is unsupported.");
        try {
            dep.downloadDependency();
            addLoadedLib(dep.maven().get().getDependencyPath().toUri().toURL());
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong while trying to load dependencies", e);
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
