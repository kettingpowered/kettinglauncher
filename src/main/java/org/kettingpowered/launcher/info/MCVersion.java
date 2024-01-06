package org.kettingpowered.launcher.info;

import org.kettingpowered.ketting.internal.KettingFiles;
import org.kettingpowered.launcher.ParsedArgs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * @author C0D3 M4513R
 */
public class MCVersion {
    private static String mc;
    public static String getMc(ParsedArgs args){
        if (mc != null) return mc;
        //If you want to manually specify the version
        //via args
        if (args.minecraftVersion() != null) {
            mc = args.minecraftVersion();
            return mc;
        }
        //Get the active ketting forge dir
        {
//            if (KettingFiles.FORGE_UNIVERSAL_JAR.exists()) {
//                new JarFile(KettingFiles.FORGE_UNIVERSAL_JAR)
//            }
        }
        //Get the last saved mc version
        {
            final InputStream mcv = MCVersion.class.getResourceAsStream("/minecraftVersion");
            if (mcv != null){
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(mcv))){
                    mc = reader.readLine().trim();
                    return mc;
                } catch (IOException ignored) {
                }
            }
        }
        //Get the version via mcp mappings, since they are not wiped.
        {
            File mcp_dir = new File(KettingFiles.LIBRARIES_PATH, "de/oceanlabs/mcp_config");
            File[] mcp_versions = mcp_dir.listFiles();
            List<String> mc_versions = Arrays.stream(Objects.requireNonNullElse(mcp_versions, new File[0]))
                    .filter(Objects::nonNull)
                    .map(File::getName)
                    .map(name -> name.substring(name.indexOf('-')))
                    .distinct()
                    .toList();
            if (mc_versions.size() == 1){
                mc = mc_versions.get(0);
                System.out.println("Inferring minecraft version '"+mc+"' based on the mapping configuration present in the libraries folder.");
                return mc;
            } else if (mc_versions.size() > 1) {
                System.out.println("There are multiple mapping configurations for multiple minecraft versions present in the libraries folder.");
            }
        }
        //and now we are out of options.
        System.err.println("""
                Could not determine the active server minecraft version. Please specify it, by specifying one of the following:
                 - the '--minecraftVersion' argument. E.g.: add ' --minecraftVersion 1.20.4 ' after the '-jar' argument
                 - the java property 'kettinglauncher.minecraftVersion' E.g.: ' -Dkettinglauncher.minecraftVersion=1.20.4 ' before the '-jar' argument
                 - the environment variable 'kettinglauncher_minecraftVersion' E.g. ' kettinglauncher_minecraftVersion=1.20.4 ' before the java executable.""");
        System.exit(1);
        throw new RuntimeException();//bogus, but has to be there to stop the compiler from complaining, that there is no return value here.
    }
}
