package org.kettingpowered.launcher.dependency;

import org.kettingpowered.launcher.KettingLauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AvailableMavenRepos {
    public static final String SERVER_RELEASES = "https://reposilite.c0d3m4513r.com/Ketting-Server-Releases/";
    static {
        List<String> instance = new ArrayList<>();
        if (KettingLauncher.Bundled) instance.add("file://"+System.getProperty("user.home")+"/.m2/repository/");
        instance.add("https://reposilite.c0d3m4513r.com/Ketting/");
        instance.add(SERVER_RELEASES);
        instance.add("https://reposilite.c0d3m4513r.com/Magma/");
        instance.add("https://repo1.maven.org/maven2/");
        instance.add("https://libraries.minecraft.net/");
        instance.add("https://maven.minecraftforge.net/");
        INSTANCE = Collections.unmodifiableList(instance);
    }
    public static final List<String> INSTANCE;
}
