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

        boolean enableUpdate = !(containsArg("-dau") || containsArg("-daus"));
        boolean enableServerUpdate = !containsArg("-daul");
        @NotNull String target = Objects.requireNonNullElse(getArg("--launchTarget"), "forge_server");
        @Nullable String minecraftVersion = getArg("--minecraftVersion");
        return new ParsedArgs(Collections.unmodifiableList(args), enableUpdate, enableServerUpdate, target, minecraftVersion);
    }

    private @Nullable String getArg(String arg) {
        if (args.isEmpty()) return null ;

        int index = args.indexOf(arg);
        if (index < 0) return null;
        args.remove(index); //remove arg
        return args.remove(index); //this should be the value to that arg
    }

    private boolean containsArg(String arg) {
        if (args.isEmpty()) return false;

        int index = args.indexOf(arg);
        if (index < 0) return false;
        args.remove(index);
        return true;
    }
}
