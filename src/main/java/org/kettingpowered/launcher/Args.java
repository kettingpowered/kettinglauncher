package org.kettingpowered.launcher;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.launcher.betterui.BetterUI;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

        boolean dau = containsArg("-dau");
        boolean daus = containsArg("-daus");
        boolean enableServerUpdate = !(dau||daus);
        boolean enableLauncherUpdate = !containsArg("-daul") && !"dev-env".equals(KettingLauncher.Version);
        @NotNull String target = Objects.requireNonNullElse(getArg("--launchTarget"), "forge_server");
        @Nullable String minecraftVersion = getArg("--minecraftVersion");
        return new ParsedArgs(Collections.unmodifiableList(args), enableServerUpdate, enableLauncherUpdate, target, minecraftVersion);
    }
    
    private static void printHelp(){
        System.out.println(KettingConstants.NAME + " Help");
        System.out.println("-".repeat(KettingConstants.NAME.length() + 5));
        System.out.println("Usage: java -jar your-jar-name.jar [options]");
        System.out.println("Options:");
        System.out.println("  -help               Shows this help message");
        System.out.println("  -noui               Disables the fancy UI");
        System.out.println("  -nologo             Disables the big logo");
        System.out.println("  -accepteula         Accepts the EULA automatically");
        System.out.println("  -dau or -daus       Disables automatic server updates");
        System.out.println("  -daul               Disables automatic launcher updates");
        System.out.println("  --launchTarget      Sets the default launchTarget. Exists mostly for debug purposes.");
        System.out.println("  --minecraftVersion  Sets the Minecraft Version of the Server. Will update the server to this version, if a Ketting version on another Minecraft Version is used.");
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
        if (System.getProperty("kettinglauncher."  + arg) != null) return true;
        return false;
    }
}
