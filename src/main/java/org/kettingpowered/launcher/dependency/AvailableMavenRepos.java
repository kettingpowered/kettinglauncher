package org.kettingpowered.launcher.dependency;

import org.kettingpowered.launcher.KettingLauncher;
import org.kettingpowered.launcher.lang.I18n;
import org.kettingpowered.launcher.log.LogLevel;
import org.kettingpowered.launcher.log.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AvailableMavenRepos {
    static {
        List<String> instance = new ArrayList<>();
        //repo overrides
        try {
            File repos = new File("maven_repos.txt");
            if (repos.exists() && repos.isFile()) {
                List<String> customRepos = new BufferedReader(new FileReader(repos)).lines().filter(v -> !v.isEmpty()).toList();
                Logger.log(LogLevel.INFO, I18n.get("info.maven.repo_overrides"), String.join(", ", customRepos));
                instance.addAll(customRepos);
            }
        }catch (Throwable e) {
            Logger.log(I18n.get("error.maven.override_file_failed"), e);
        }
        
        if (KettingLauncher.Bundled) instance.add("file://"+System.getProperty("user.home")+"/.m2/repository/");

        //main repo
        instance.add("https://reposilite.c0d3m4513r.com/Ketting/");
        instance.add("https://reposilite.c0d3m4513r.com/Ketting-Server-Releases/");
        instance.add("https://reposilite.c0d3m4513r.com/Magma/");

        //backup repo
        instance.add("https://repo.kettingpowered.org/Ketting/");
        instance.add("https://repo.kettingpowered.org/Ketting-Server-Releases/");
        instance.add("https://repo.kettingpowered.org/Magma/");

        //general libs
        instance.add("https://repo1.maven.org/maven2/");
        instance.add("https://libraries.minecraft.net/");
        instance.add("https://maven.minecraftforge.net/");
        INSTANCE = Collections.unmodifiableList(instance);
    }
    public static final List<String> INSTANCE;
}
