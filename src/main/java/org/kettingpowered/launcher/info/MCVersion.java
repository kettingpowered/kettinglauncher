package org.kettingpowered.launcher.info;

import org.kettingpowered.launcher.KettingFiles;
import org.kettingpowered.launcher.ParsedArgs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
        //via a Property
        {
            final String prop = System.getProperty("minecraftVersion");
            if (prop != null) {
                mc = prop;
                return prop;
            }
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
            if (mcp_versions != null && mcp_versions.length == 1){
                String mcp_version = mcp_versions[0].getName();
                mc = mcp_version.substring(mcp_version.indexOf('-'));
                return mc;
            }
        }
        //and now we are out of options.
        throw new RuntimeException("Could not find minecraftVersion. ");
    }
}
