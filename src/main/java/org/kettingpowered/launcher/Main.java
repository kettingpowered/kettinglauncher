package org.kettingpowered.launcher;

import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.ketting.internal.KettingFiles;
import org.kettingpowered.launcher.dependency.Dependency;
import org.kettingpowered.launcher.dependency.Libraries;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarFile;

public class Main {
    public static final boolean DEBUG = "true".equals(System.getProperty("kettinglauncher.debug"));
    public static final String FORGE_SERVER_ARTIFACT_ID = "forge";
    public static void agentmain(String agentArgs, Instrumentation inst) throws IOException {
        if (DEBUG) System.out.println("[Agent] premain lib load start");
        final Libraries libs = new Libraries();
        //Download all needed libs for the Launcher itself
        try (BufferedReader stream = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("data/launcher_libraries.txt"))))){
            libs.downloadExternal(stream.lines().map(Dependency::parse).filter(Optional::isPresent).map(Optional::get).toList(), false, "SHA-512");
        }

        Arrays.stream(libs.getLoadedLibs()).forEach(url -> {
            try {
                inst.appendToSystemClassLoaderSearch(new JarFile(new File(url.toURI())));
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        if (DEBUG) System.out.println("[Agent] premain lib load end");
    }
    public static void main(String[] args) throws Exception {
        final KettingLauncher launcher = new KettingLauncher(args);
        launcher.init();
        launcher.launch();
    }
}
