package org.kettingpowered.launcher.dependency;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.kettingpowered.launcher.betterui.BetterUI;
import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.launcher.KettingFiles;
import org.kettingpowered.launcher.internal.utils.NetworkUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    
    private void loadDep(Dependency dep){
        try {
            LibHelper.downloadDependency(dep);
            addLoadedLib(LibHelper.getDependencyPath(dep).toUri().toURL());
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong while trying to load dependencies", e);
        }
    }

    public static void downloadMcp() throws IOException {
        if (KettingFiles.MCP_ZIP.exists()) return;

        String mcMcp = KettingConstants.MINECRAFT_VERSION + "-" + KettingConstants.MCP_VERSION;

        try {
            //noinspection ResultOfMethodCallIgnored
            KettingFiles.MCP_ZIP.getParentFile().mkdirs();
            String mavenBasePath = "de/oceanlabs/mcp/mcp_config/" + mcMcp + "/mcp_config-" + mcMcp + ".zip";

            for (String repo : AvailableMavenRepos.INSTANCE) {
                try {
                    String fullPath = repo + mavenBasePath;
                    String hash = NetworkUtils.readFile(fullPath + ".sha512");
                    NetworkUtils.downloadFile(fullPath, KettingFiles.MCP_ZIP, hash, "SHA-512");
                    break;
                } catch (Throwable ignored) {
                    if (AvailableMavenRepos.isLast(repo)) {
                        System.err.println("Failed to download mcp_config from any repo, check your internet connection and try again.");
                        System.exit(1);
                    }

                    System.err.println("Failed to download " + mavenBasePath + " from " + repo + ", trying next repo");
                }
            }
        } catch (Exception e) {
            //noinspection ResultOfMethodCallIgnored
            KettingFiles.MCP_ZIP.delete();
            throw new IOException("Failed to download MCP", e);
        }
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
