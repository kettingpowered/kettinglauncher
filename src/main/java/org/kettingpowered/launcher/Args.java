package org.kettingpowered.launcher;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.launcher.betterui.BetterUI;
import org.kettingpowered.launcher.log.LogLevel;
import org.kettingpowered.launcher.log.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author C0D3 M4513R
 */
public class Args {
    
    private final @NotNull List<String> args;

    public Args(String[] args) {
        this.args = new ArrayList<>(List.of(args));
    }
    
    public ParsedArgs parse(BetterUI ui, Path eula) throws IOException {
        if (containsArg("-help") || containsArg("--help")) {
            printHelp();
            System.exit(0);
        }

        if (containsArg("-noui"))
            ui.setEnabled(false);

        if (containsArg("-nologo"))
            ui.setEnableBigLogo(false);

        if (containsArg("-accepteula"))
            BetterUI.forceAcceptEULA(eula);

        boolean createLaunchScripts = containsArg("-createLaunchScripts");
        boolean installOnly = containsArg("-installOnly") || createLaunchScripts; //run install if createLaunchScripts is enabled
        boolean dau = containsArg("-dau");
        boolean daus = containsArg("-daus");
        boolean enableServerUpdate = !(dau||daus);
        boolean enableLauncherUpdate = !containsArg("-daul") && !"dev-env".equals(KettingLauncher.Version);
        @Nullable String target = getArg("--launchTarget");
        @Nullable String minecraftVersion = getArg("-minecraftVersion");
        @Nullable String forgeVersion = getArg("-forgeVersion");
        @Nullable String kettingVersion = getArg("-kettingVersion");
        //backward compatibility
        if (minecraftVersion == null) {
            minecraftVersion = getArg("-minecraftVersion");
            if (minecraftVersion != null) Logger.log(LogLevel.WARN, "warn.discontinued.double_dash_minecraftVersion");
        }
        return new ParsedArgs(Collections.unmodifiableList(args), createLaunchScripts, installOnly, enableServerUpdate, enableLauncherUpdate, target, minecraftVersion, forgeVersion, kettingVersion);
    }
    
    private static void printHelp(){
        System.out.println(KettingConstants.NAME + " Help");
        System.out.println("-".repeat(KettingConstants.NAME.length() + 5));
        System.out.println("Usage: java -jar your-jar-name.jar [options]");
        System.out.println("Options:");
        System.out.println("  -help                Shows this help message");
        System.out.println("  -noui                Disables the fancy UI");
        System.out.println("  -nologo              Disables the big logo");
        System.out.println("  -accepteula          Accepts the EULA automatically");
        System.out.println("  -dau or -daus        Disables automatic server updates");
        System.out.println("  -daul                Disables automatic launcher updates");
        System.out.println("  -createLaunchScripts Creates launch scripts to launch Ketting directly");
        System.out.println("  -installOnly         Stop the launcher, after the server has been installed.");
        System.out.println("  -minecraftVersion    Sets the Minecraft Version of the Server. Will update the server to this version, if a Ketting version on another Minecraft Version is used.");
        System.out.println("  --minecraftVersion   See above, except it's deprecated. Swap to the one above, for the exact same effect.");
        System.out.println("  -forgeVersion        Sets the Forge Version of the Server. Will update the server to this version, if a Ketting version on another Forge or Minecraft Version is used.");
        System.out.println("  -kettingVersion      Sets the Ketting Version of the Server. Will update the server to this version, if a Ketting version on another Ketting or Minecraft Version is used.");
        System.out.println("Development|Unstable Options:");
        System.out.println("  --launchTarget      Sets the default launchTarget. Exists mostly for debug purposes for forge. We reserve the right to ignore this (e.g. if it's unsupported by the target server version )");
    }

    private @Nullable String getArg(String arg) {
        int index = args.indexOf(arg);
        if (index >= 0) {
            args.remove(index); //remove arg
            return args.remove(index); //this should be the value to that arg
        }
        arg = arg.replace("-","");
        String out = System.getenv("kettinglauncher_" + arg);
        if (out != null) return out;
        return System.getProperty("kettinglauncher."+arg);
    }

    private boolean containsArg(String arg) {
        int index = args.indexOf(arg);
        if (index >= 0){
            args.remove(index);
            return true;
        }
        arg = arg.replace("-","");
        if (System.getenv("kettinglauncher_" + arg) != null) return true;
        //noinspection RedundantIfStatement
        if (System.getProperty("kettinglauncher."  + arg) != null) return true;
        return false;
    }
}
