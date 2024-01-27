package org.kettingpowered.launcher.betterui;

import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.launcher.KettingLauncher;
import org.kettingpowered.launcher.log.LogLevel;
import org.kettingpowered.launcher.log.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class BetterUI {

    public static final int
            LOGO_LENGTH = 62,
            DIVIDER_LENGTH = 45;
    private boolean enabled = true,  enableBigLogo = true;

    private static final String MC_COLOR_DARK_GRAY = "\u00A78";

    private static final String[] bigLogo = {
            MC_COLOR_DARK_GRAY + " /##   /##             /##     /##     /##                         ",
            MC_COLOR_DARK_GRAY + "| ##  /##/            | ##    | ##    |__/                         ",
            MC_COLOR_DARK_GRAY + "| ## /##/   /######  /###### /######   /## /#######   /######      ",
            MC_COLOR_DARK_GRAY + "| #####/   /##__  ##|_  ##_/|_  ##_/  | ##| ##__  ## /##__  ##     ",
            MC_COLOR_DARK_GRAY + "| ##  ##  | ########  | ##    | ##    | ##| ##  \\ ##| ##  \\ ##   ",
            MC_COLOR_DARK_GRAY + "| ##\\  ## | ##_____/  | ## /##| ## /##| ##| ##  | ##| ##  | ##    ",
            MC_COLOR_DARK_GRAY + "| ## \\  ##|  #######  |  ####/|  ####/| ##| ##  | ##|  #######    ",
            MC_COLOR_DARK_GRAY + "|__/  \\__/ \\_______/   \\___/   \\___/  |__/|__/  |__/ \\____  ##",
            MC_COLOR_DARK_GRAY + "                                                     /##  \\ ##    ",
            MC_COLOR_DARK_GRAY + "                                                    |  ######/     ",
            MC_COLOR_DARK_GRAY + "                                                     \\______/     "
    };

    private final String name;

    public BetterUI(String name){
        this.name = name;
    }
    
    public void printTitle(final String mc_version) {
        if (!enabled)
            return;

        final String divider = "-".repeat(DIVIDER_LENGTH);
        final String copyright = "Copyright (c) " + new SimpleDateFormat("yyyy").format(new Date()) + " " + KettingConstants.BRAND;
        final String java = "Running on Java " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")";
        final String launcher = "Launcher version "+ KettingLauncher.Version;
        final String minecraft = "Minecraft version "+ mc_version;
        final String server = name + " version " + KettingConstants.KETTING_VERSION;
        final String forge = "Forge version   " + KettingConstants.FORGE_VERSION;
        final String bukkit = "Bukkit version  " + KettingConstants.BUKKIT_PACKAGE_VERSION;
        if (enableBigLogo) {
            System.out.println();
            for (int i = 0; i < bigLogo.length; i++) {
                if (i < 9)
                    Logger.marker("logo", bigLogo[i]);
                else {
                    if (i == 9)
                        printPartial(copyright, bigLogo[i]);
                    else
                        printPartial(divider, bigLogo[i]);
                }
            }
        } else {
            Logger.marker("logo", name);
            Logger.marker("logo", copyright);
            Logger.marker("logo", divider);
        }

        Logger.marker("logo", java);
        Logger.marker("logo", launcher);
        Logger.marker("logo", minecraft);
        Logger.marker("logo", server);
        Logger.marker("logo", forge);
        Logger.marker("logo", bukkit);
        Logger.marker("logo", divider);
    }

    private static void printPartial(String s, String logo) {
        int l = Math.min(s.length(), DIVIDER_LENGTH);
        Logger.marker("logo", s.substring(0, l) + MC_COLOR_DARK_GRAY + logo.substring(l + MC_COLOR_DARK_GRAY.length()));
    }

    public boolean checkEula(Path path_to_eula) throws IOException {
        File file = path_to_eula.toFile();
        ServerEula eula = new ServerEula(path_to_eula);

        if (!enabled)
            return eula.hasAgreedToEULA();

        if (!eula.hasAgreedToEULA()) {
            Logger.log(LogLevel.WARN, "WARNING: It appears you have not agreed to the EULA.\nPlease read the EULA (https://www.minecraft.net/eula) and type 'yes' to continue.");
            System.out.print("Do you accept? (yes/no): ");

            int wrong = 0;

            Scanner console = new Scanner(System.in);
            while (true) {
                String answer = console.nextLine();
                if (answer == null || answer.isBlank()) {
                    if (wrong++ >= 2) {
                        Logger.log(LogLevel.ERROR, "You have typed the wrong answer too many times. Exiting.");
                        return false;
                    }
                    Logger.log(LogLevel.WARN, "Please type 'yes' or 'no'.");
                    System.out.print("Do you accept? (yes/no): ");
                    continue;
                }

                switch (answer.toLowerCase()) {
                    case "y", "yes" -> {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                        //noinspection ResultOfMethodCallIgnored
                        file.createNewFile();
                        try (FileWriter writer = new FileWriter(file)) {
                            writer.write("eula=true");
                        }
                        return true;
                    }
                    case "n", "no" -> {
                        Logger.log(LogLevel.ERROR, "You must accept the EULA to continue. Exiting.");
                        return false;
                    }
                    default -> {
                        if (wrong++ >= 2) {
                            Logger.log(LogLevel.ERROR, "You have typed the wrong answer too many times. Exiting.");
                            return false;
                        }
                        Logger.log(LogLevel.WARN, "Please type 'yes' or 'no'.");
                        System.out.print("Do you accept? (yes/no): ");
                    }
                }
            }

        } else return true;
    }

    public static void forceAcceptEULA(Path path_to_eula) throws IOException {
        File file = path_to_eula.toFile();
        if (file.exists())
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        //noinspection ResultOfMethodCallIgnored
        file.createNewFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("eula=true");
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnableBigLogo(boolean enableBigLogo) {
        this.enableBigLogo = enableBigLogo;
    }

}

