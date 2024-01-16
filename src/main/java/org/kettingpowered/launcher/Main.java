package org.kettingpowered.launcher;

import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.launcher.dependency.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarFile;

/**
 * @author C0D3 M4513R
 */
public class Main {
    public static final File LauncherJar = new File(URLDecoder.decode(Main.class.getProtectionDomain().getCodeSource().getLocation().getFile(), StandardCharsets.UTF_8));
    public static final File LauncherDir = LauncherJar.getParentFile();
    public static final boolean DEBUG = "true".equals(System.getProperty("kettinglauncher.debug"));
    public static final String FORGE_SERVER_ARTIFACT_ID = "forge";
    //This is used in a premain context in LibHelper, where KettingCommon might not be available yet.
    //Java is VERY nice however and inlines this at compile-time, saving us the trouble of defining this twice.
    //This will only pull the INSTALLER_LIBRARIES_FOLDER from the compileTime KettingConstants version.
    public static final String INSTALLER_LIBRARIES_FOLDER = KettingConstants.INSTALLER_LIBRARIES_FOLDER;
    public static final MavenArtifact KETTINGCOMMON = new MavenArtifact(KettingConstants.KETTING_GROUP, "kettingcommon", "1.0.0", Optional.empty(), Optional.of("jar"));


    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        if (DEBUG) System.out.println("[Agent] premain lib load start");
        URL urlKettingCommon;
        List<Object> dependencyList;

        //load kettingcore
        try (URLClassLoader loader = new AgentClassLoader(new URL[]{Main.LauncherJar.toURI().toURL()})) {
            Object[] tuple = (Object[]) Class.forName(Main.class.getName(), true, loader)
                    .getDeclaredMethod("agent_pre_kettingcommon")
                    .invoke(null);
            //noinspection unchecked
            dependencyList = (List<Object>) tuple[0];
            urlKettingCommon = (URL) tuple[1];
        }

        //then load the dependencies we need
        try (URLClassLoader loader = new AgentClassLoader(new URL[] {Main.LauncherJar.toURI().toURL(), urlKettingCommon})){
            Class.forName(Main.class.getName(), true, loader)
                    .getDeclaredMethod("agent_post_kettingcommon", List.class, Instrumentation.class)
                    .invoke(null, dependencyList, inst);
        }

        if (DEBUG) System.out.println("[Agent] premain lib load end");
    }
    
    @SuppressWarnings("unused")
    public static Object[] agent_pre_kettingcommon() throws Exception{
        List<Dependency<MavenArtifact>> dependencyList;
        //Download all needed libs for the Launcher itself
        try (BufferedReader stream = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("data/launcher_libraries.txt"))))){
            dependencyList = stream.lines()
                    .map(Dependency::parse)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        }
        Dependency kettingcommon = dependencyList.stream()
                .filter(dep->{
                    try {
                        return dep.maven().equalsIgnoringVersion(Main.KETTINGCOMMON);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(Dependency[]::new)[0];
        kettingcommon.download();
        URL urlKettingCommon = Maven.getDependencyPath(kettingcommon.maven().getPath()).toAbsolutePath().toUri().toURL();
        return new Object[]{dependencyList, urlKettingCommon};
    }

    @SuppressWarnings({"unused", "unchecked"})
    public static void agent_post_kettingcommon(List<Object> dependencyList, Instrumentation inst) {
        final Libraries libs = new Libraries();
        List<Dependency<MavenArtifact>> deps = dependencyList.stream()
                .map(obj->{
                    try{
                        Class<?> otherDepClass = obj.getClass();
                        return new Dependency<>(
                                reconstructHash(otherDepClass.getDeclaredMethod("hash").invoke(obj)),
                                reconstructMavenArtifact(otherDepClass.getDeclaredMethod("maven").invoke(obj))
                        );
                    }catch (Exception e){
                        throw new RuntimeException(e);
                    }
                }).toList();
        libs.downloadExternal(deps, false);
        Arrays.stream(libs.getLoadedLibs()).forEach(url -> {
            try {
                inst.appendToSystemClassLoaderSearch(new JarFile(new File(url.toURI())));
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    private static MavenArtifact reconstructMavenArtifact(Object maven) throws Exception{
        Class<?> otherMavenClass = maven.getClass();
        Field group = otherMavenClass.getDeclaredField("group");
        Field artifactId = otherMavenClass.getDeclaredField("artifactId");
        Field version = otherMavenClass.getDeclaredField("version");
        Field classifier = otherMavenClass.getDeclaredField("classifier");
        Field extension = otherMavenClass.getDeclaredField("extension");
        group.setAccessible(true);
        artifactId.setAccessible(true);
        version.setAccessible(true);
        classifier.setAccessible(true);
        extension.setAccessible(true);
        return new MavenArtifact(
                (String) group.get(maven),
                (String) artifactId.get(maven),
                (String) version.get(maven),
                (Optional<String>) classifier.get(maven),
                (Optional<String>) extension.get(maven)
        );
    }

    private static Hash reconstructHash(Object maven) throws Exception{
        Class<?> otherMavenClass = maven.getClass();
        Field hash = otherMavenClass.getDeclaredField("hash");
        Field algorithm = otherMavenClass.getDeclaredField("algorithm");
        hash.setAccessible(true);
        algorithm.setAccessible(true);
        return new Hash(
                (String) hash.get(maven),
                (String) algorithm.get(maven)
        );
    }

    public static void main(String[] args) throws Exception {
        final KettingLauncher launcher = new KettingLauncher(args);
        launcher.init();
        launcher.launch();
    }
}
