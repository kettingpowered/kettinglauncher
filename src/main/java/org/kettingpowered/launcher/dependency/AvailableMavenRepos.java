package org.kettingpowered.launcher.dependency;

import java.util.List;

public final class AvailableMavenRepos {
    public static final String SERVER_RELEASES = "https://nexus.c0d3m4513r.com/repository/Ketting-Server-Releases/";
    public static final List<String> INSTANCE = List.of(
            "https://nexus.c0d3m4513r.com/repository/Ketting/",
            SERVER_RELEASES,
            "https://nexus.c0d3m4513r.com/repository/Magma/",
            "https://repo1.maven.org/maven2/",
            "https://libraries.minecraft.net/",
            "https://maven.minecraftforge.net/"
    );

    public static boolean isLast(String repo) {
        return INSTANCE.get(INSTANCE.size() - 1).equals(repo);
    }
}
