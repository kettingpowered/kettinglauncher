package org.kettingpowered.launcher;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
            ui.printHelp();
            System.exit(0);
        }

        if (containsArg("-noui"))
            ui.setEnabled(false);

        if (containsArg("-nologo"))
            ui.setEnableBigLogo(false);

        if (containsArg("-accepteula"))
            BetterUI.forceAcceptEULA(eula);

        boolean enableServerUpdate = !(containsArg("-dau") || containsArg("-daus"));
        boolean enableLauncherUpdate = !"dev-env".equals(KettingLauncher.Version) && !containsArg("-daul");
        @NotNull String target = Objects.requireNonNullElse(getArg("--launchTarget"), "forge_server");
        @Nullable String minecraftVersion = getArg("--minecraftVersion");
        return new ParsedArgs(Collections.unmodifiableList(args), enableServerUpdate, enableLauncherUpdate, target, minecraftVersion);
    }

    private @Nullable String getArg(String arg) {
        int index = args.indexOf(arg);
        if (index > 0) {
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
        if (index >= 0) return true;
        arg = arg.replace("-","");
        if (System.getenv("kettinglauncher_" + arg) != null) return true;
        if (System.getProperty("kettinglauncher."  + arg) != null) return true;
        return false;
    }
}
