package org.kettingpowered.launcher.dependency;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.launcher.KettingLauncher;
import org.kettingpowered.launcher.betterui.BetterUI;
import org.kettingpowered.launcher.lang.I18n;

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

/**
 * @author C0D3 M4513R
 */
public class Libraries {
    private final List<URL> loadedLibs = new ArrayList<>();
    
    public Libraries() {}
    public void downloadExternal(List<Dependency<MavenArtifact>> dependencies, boolean progressBar) {
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
        }
        
        dependencies.parallelStream()
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
    
    private File downloadDep(Dependency<MavenArtifact> dep) {
        if (KettingLauncher.Bundled && KettingConstants.KETTINGSERVER_GROUP.equals(dep.maven().group())) {
            I18n.logDebug("info.libraries.skip_bundled", dep);
            return Maven.getDependencyPath(dep.maven().getPath()).toFile().getAbsoluteFile();
        }
        File depFile;
        try{
            if (!KettingLauncher.Bundled && KettingLauncher.AUTO_UPDATE_LIBS.stream().anyMatch(artifact -> dep.maven().equalsIgnoringVersion(artifact))) {
                MavenArtifact artifact = dep.maven().getLatestMinorPatch();
                I18n.logDebug("info.libraries.using_different", artifact, dep.maven());
                if (!artifact.equals(dep.maven())) depFile = artifact.download();
                else depFile = dep.download();
            }else {
                depFile = dep.download();
            }
            return depFile;
        }catch (Exception e){
            throw new RuntimeException("Something went wrong whilst trying to load dependencies", e);
        }
    }

    public static void downloadMcp() throws Exception {
        String mcMcp = KettingConstants.MINECRAFT_VERSION + "-" + KettingConstants.MCP_VERSION;
        MavenArtifact mcp_artifact = new MavenArtifact("de.oceanlabs.mcp", "mcp_config", mcMcp, Optional.empty(), Optional.of("zip"));
        mcp_artifact.download();
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
            I18n.logError("error.libraries.failed_to_load");
            System.exit(1);
        }

        return loadedLibs.toArray(new URL[0]);
    }
}
