package org.kettingpowered.launcher.dependency;

import org.kettingpowered.launcher.KettingLauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AvailableMavenRepos {
    public static final String SERVER_RELEASES = "https://nexus.c0d3m4513r.com/repository/Ketting-Server-Releases/";
    static {
        List<String> instance = new ArrayList<>();
        if (KettingLauncher.Bundled) instance.add("file://"+System.getProperty("user.home")+"/.m2/repository/");
        instance.add("https://nexus.c0d3m4513r.com/repository/Ketting/");
        instance.add(SERVER_RELEASES);
        instance.add("https://nexus.c0d3m4513r.com/repository/Magma/");
        instance.add("https://repo1.maven.org/maven2/");
        instance.add("https://libraries.minecraft.net/");
        instance.add("https://maven.minecraftforge.net/");
        instance.add("https://maven.neoforged.net/");
        INSTANCE = Collections.unmodifiableList(instance);
    }
    public static final List<String> INSTANCE;
}
