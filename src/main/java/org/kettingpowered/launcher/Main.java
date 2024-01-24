package org.kettingpowered.launcher;

import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.ketting.internal.Tuple;
import org.kettingpowered.launcher.dependency.*;
import org.kettingpowered.launcher.lang.I18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

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
    static Instrumentation INST;
    private static KettingLauncher launcher = null;
    private static List<String> jvmArg = new ArrayList<>();
    private static List<String> procArg = new ArrayList<>();

    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        if (DEBUG) System.out.println("[Agent] premain lib load start");

        //Load language files
        I18n.load();

        INST = inst;
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

        List<String> args = List.of(ProcessHandle.current().info().arguments().orElseGet(()->new String[0]));
        int index = args.indexOf("-jar");
        if (index >= 0){
            jvmArg = args.subList(0, index);
            int procArgStart = Math.min(index+2, args.size()-1);
            procArg = args.subList(procArgStart, args.size());
        }
        launcher = new KettingLauncher(procArg.toArray(String[]::new));
        launcher.init();
        launcher.prepareLaunch();
    }
    
    @SuppressWarnings("unused")
    public static Object[] agent_pre_kettingcommon() throws Exception{
        //Damn classloaders
        I18n.load(true);

        List<Dependency<MavenArtifact>> dependencyList;
        //Download all needed libs for the Launcher itself
        try (BufferedReader stream = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("data/launcher_libraries.txt"))))){
            dependencyList = stream.lines()
                    .map(Dependency::parse)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        }
        Dependency<?> kettingcommon = dependencyList.stream()
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

    @SuppressWarnings("unused")
    public static void agent_post_kettingcommon(List<Object> dependencyList, Instrumentation inst) {
        //Damn classloaders
        I18n.load(true);

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
        String main = launcher.launch();
        final var defaultArgs = getDefaultArgs(launcher.args, launcher.libs, main);
        List<String> newArgs = new ArrayList<>(jvmArg);
        newArgs.addAll(defaultArgs.t1());
        newArgs.add("-Dketting.remapper.dump=./.mixin.out/plugin_classes");
        newArgs.add(main);
        newArgs.addAll(defaultArgs.t2());

        if (Main.DEBUG) System.out.println(I18n.get("debug.launching") + Arrays.toString(newArgs.toArray()));
        Runtime.getRuntime().gc();

        new ProcessBuilder(newArgs)
                .inheritIO()
                .start();
        //purposely exit here. Some people will add args that specify a minimum ram allocation.
    }
    
    private static Tuple<List<String>, List<String>> getDefaultArgs(ParsedArgs args, Libraries libraries, String main){
        //noinspection EnhancedSwitchMigration
        switch (main) {
            case "cpw.mods.bootstraplauncher.BootstrapLauncher": 
                return new Tuple<>(
                    Arrays.asList(
                            "-p",
                            Arrays.stream(libraries.getLoadedLibs())
                                    .filter(url -> 
                                            url.toString().contains("org/ow2/asm") || 
                                            url.toString().contains("cpw/mods/securejarhandler") || 
                                            url.toString().contains("cpw/mods/bootstraplauncher") || 
                                            url.toString().contains("net/minecraftforge/JarJarFileSystems")
                                    ).map(url-> {
                                        try {
                                            return url.toURI();
                                        } catch (URISyntaxException e) {
                                            return null;
                                        }
                                    }).filter(Objects::nonNull)
                                    .map(dep->new File(dep).getAbsolutePath())
                                    .collect(Collectors.joining(File.pathSeparator)),
                            "--add-modules ALL-MODULE-PATH",
                            "--add-opens java.base/java.util.jar=cpw.mods.securejarhandler",
                            "--add-opens java.base/java.lang.invoke=cpw.mods.securejarhandler",
                            "--add-exports java.base/sun.security.util=cpw.mods.securejarhandler",
                            "--add-exports jdk.naming.dns/com.sun.jndi.dns=java.naming",
                            "-DlegacyClassPath="+System.getProperty("java.class.path"),
                            "-DlibraryDirectory=libraries",
                            "-Djava.net.preferIPv6Addresses=system"
                        ),
                        Arrays.asList(
                                "--launchTarget",
                                args.launchTarget()!=null? args.launchTarget() : "forgeserver",
                                "--fml.forgeVersion",
                                KettingConstants.FORGE_VERSION,
                                "--fml.mcVersion",
                                KettingConstants.MINECRAFT_VERSION,
                                "--fml.forgeGroup",
                                "net.minecraftforge",
                                "--fml.mcpVersion",
                                KettingConstants.MCP_VERSION
                        )
                );
            case  "net.minecraftforge.bootstrap.ForgeBootstrap":
            default:
                return new Tuple<>(
                        Collections.emptyList(),
                        List.of(
                                "--launchTarget", 
                                args.launchTarget()!=null? args.launchTarget() : "forge_server"
                        )
                );
        }
    }
}