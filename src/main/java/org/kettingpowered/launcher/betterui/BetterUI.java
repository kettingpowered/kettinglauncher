package org.kettingpowered.launcher.betterui;

import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.launcher.KettingLauncher;

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

    private static final String[] bigLogo = {
            " /##   /##             /##     /##     /##                         ",
            "| ##  /##/            | ##    | ##    |__/                         ",
            "| ## /##/   /######  /###### /######   /## /#######   /######      ",
            "| #####/   /##__  ##|_  ##_/|_  ##_/  | ##| ##__  ## /##__  ##     ",
            "| ##  ##  | ########  | ##    | ##    | ##| ##  \\ ##| ##  \\ ##   ",
            "| ##\\  ## | ##_____/  | ## /##| ## /##| ##| ##  | ##| ##  | ##    ",
            "| ## \\  ##|  #######  |  ####/|  ####/| ##| ##  | ##|  #######    ",
            "|__/  \\__/ \\_______/   \\___/   \\___/  |__/|__/  |__/ \\____  ##",
            "                                                     /##  \\ ##    ",
            "                                                    |  ######/     ",
            "                                                     \\______/     "
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
        final String server = name + " version " + KettingConstants.VERSION;
        final String bukkit = "Bukkit version  " + KettingConstants.BUKKIT_PACKAGE_VERSION;
        final String forge = "Forge version   " + KettingConstants.FORGE_VERSION;
        if (enableBigLogo) {
            System.out.println();
            for (int i = 0; i < bigLogo.length; i++) {
                if (i < 9)
                    System.out.println(bigLogo[i]);
                else {
                    if (i == 9)
                        printPartial(copyright, bigLogo[i]);
                    else
                        printPartial(divider, bigLogo[i]);
                }
            }
        } else {
            System.out.println(name);
            System.out.println(copyright);
            System.out.println(divider);
        }

        System.out.println(java);
        System.out.println(launcher);
        System.out.println(minecraft);
        System.out.println(server);
        System.out.println(bukkit);
        System.out.println(forge);
        System.out.println(divider);
    }

    private static void printPartial(String s, String logo) {
        int l = Math.min(s.length(), DIVIDER_LENGTH);
        System.out.println(s.substring(0, l) + logo.substring(l));
    }

    public boolean checkEula(Path path_to_eula) throws IOException {
        File file = path_to_eula.toFile();
        ServerEula eula = new ServerEula(path_to_eula);

        if (!enabled)
            return eula.hasAgreedToEULA();

        if (!eula.hasAgreedToEULA()) {
            System.out.println("WARNING: It appears you have not agreed to the EULA.\nPlease read the EULA (https://www.minecraft.net/eula) and type 'yes' to continue.");
            System.out.print("Do you accept? (yes/no): ");

            int wrong = 0;

            Scanner console = new Scanner(System.in);
            while (true) {
                String answer = console.nextLine();
                if (answer == null || answer.isBlank()) {
                    if (wrong++ >= 2) {
                        System.err.println("You have typed the wrong answer too many times. Exiting.");
                        return false;
                    }
                    System.out.println("Please type 'yes' or 'no'.");
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
                        System.err.println("You must accept the EULA to continue. Exiting.");
                        return false;
                    }
                    default -> {
                        if (wrong++ >= 2) {
                            System.err.println("You have typed the wrong answer too many times. Exiting.");
                            return false;
                        }
                        System.out.println("Please type 'yes' or 'no'.");
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

