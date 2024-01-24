package org.kettingpowered.launcher.info;

import org.jetbrains.annotations.Nullable;
import org.kettingpowered.ketting.internal.KettingFiles;
import org.kettingpowered.ketting.internal.MajorMinorPatchVersion;
import org.kettingpowered.ketting.internal.Tuple;
import org.kettingpowered.launcher.ParsedArgs;
import org.kettingpowered.launcher.utils.FileUtils;

import java.io.*;
import java.util.*;

/**
 * @author C0D3 M4513R
 */
public class MCVersion {
    private static String mc;
    public static String getMc(ParsedArgs args, List<String> supportedMcVersions){
        if (mc != null) return mc;

        //Check for a mcversion.txt file, this has priority
        {
            File mcversion = new File(KettingFiles.MAIN_FOLDER_FILE, "mcversion.txt");
            if (mcversion.exists()){
                try {
                    String version = readFromIS(new FileInputStream(mcversion));
                    if (version != null) {
                        mc = version;
                        return mc;
                    }
                } catch (FileNotFoundException ignored) {} //File is known to exist, so this should never happen.
            }
        }

        //If you want to manually specify the version
        //via args
        if (args.minecraftVersion() != null) {
            mc = args.minecraftVersion();
            return mc;
        }
        //Get the active ketting forge dir
        {
            List<String> versions = KettingFiles.getKettingServerVersions();
            HashMap<MajorMinorPatchVersion<Integer>, List<Tuple<MajorMinorPatchVersion<Integer>, MajorMinorPatchVersion<Integer>>>> map = MajorMinorPatchVersion.parseKettingServerVersionList(versions.stream());
            List<MajorMinorPatchVersion<Integer>> mc_versions = new ArrayList<>(map.keySet().stream().toList());
            mc_versions.sort(Comparator.comparing((MajorMinorPatchVersion<Integer> t) -> t).reversed()); //sort, so that index 0 is the largest minecraft version
            if (!mc_versions.isEmpty() && !map.get(mc_versions.get(0)).isEmpty()) {
                final MajorMinorPatchVersion<Integer> mc_version = mc_versions.get(0);
                map.get(mc_version).sort(Comparator.comparing((Tuple<MajorMinorPatchVersion<Integer>, MajorMinorPatchVersion<Integer>> t) -> t.t1()).thenComparing(Tuple::t2).reversed());
                final Tuple<MajorMinorPatchVersion<Integer>, MajorMinorPatchVersion<Integer>> forge_ketting_version = map.get(mc_version).get(0);
                try{
                    Arrays.stream(Objects.requireNonNullElse(KettingFiles.KETTINGSERVER_FORGE_DIR.listFiles(File::isDirectory), new File[0]))
                            .filter(file-> !String.format("%s-%s-%s", mc_version, forge_ketting_version.t1(), forge_ketting_version.t2()).equals(file.getName()))
                            .forEach(FileUtils::deleteDir);
                }catch (Throwable ignored){}
                return mc_versions.get(0).toString();
            }
        }
        //Get the last saved mc version
        {
            final InputStream mcv = MCVersion.class.getResourceAsStream("/minecraftVersion");
            String version = readFromIS(mcv);
            if (version != null) {
                mc = version;
                return mc;
            }
        }
        //Get the version via mcp mappings, since they are not wiped.
        {
            File mcp_dir = new File(KettingFiles.LIBRARIES_PATH, "de/oceanlabs/mcp_config");
            File[] mcp_versions = mcp_dir.listFiles();
            List<String> mc_versions = Arrays.stream(Objects.requireNonNullElse(mcp_versions, new File[0]))
                    .filter(Objects::nonNull)
                    .map(File::getName)
                    .map(name -> name.substring(0, name.indexOf('-')))
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
        //Ask for a version
        {
            System.out.println("Could not automatically determine the minecraft version.");
            System.out.println("Please enter the minecraft version you want to use");
            System.out.println("The following versions are supported: " + String.join(", ", supportedMcVersions));
            System.out.print("Minecraft version: ");

            int wrong = 0;

            Scanner console = new Scanner(System.in);
            while (true) {
                String answer = console.nextLine();
                if (answer == null || answer.isBlank()) {
                    if (wrong++ >= 2) {
                        System.err.println("You have typed the wrong answer too many times. Exiting.");
                        System.exit(1);
                    }
                    System.err.println("Please enter a valid version.");
                    System.out.print("Minecraft version: ");
                    continue;
                }
                if (supportedMcVersions.contains(answer.trim())) {
                    mc = answer;
                    return mc;
                }
                System.out.println("The version you entered is not supported.");
                System.out.print("Minecraft version: ");
            }
        }
    }

    private static String readFromIS(@Nullable InputStream versionStream) {
        if (versionStream == null) return null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(versionStream))){
            return reader.readLine().trim();
        } catch (IOException ignored) {
            return null;
        }
    }
}
