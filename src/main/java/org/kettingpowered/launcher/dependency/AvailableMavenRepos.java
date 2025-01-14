package org.kettingpowered.launcher.dependency;

import org.kettingpowered.launcher.KettingLauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AvailableMavenRepos {
    static {
        List<String> instance = new ArrayList<>();
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
