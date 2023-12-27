package org.kettingpowered.launcher.dependency;

import org.jetbrains.annotations.NotNull;
import java.util.Optional;

public record Dependency(String hash, Optional<MavenArtifact> maven) {
    public static @NotNull Optional<Dependency> parse(String line){
        final String[] sep = line.trim().split("\t");
        if (sep.length < 2) return Optional.empty();
        return Optional.of(new Dependency(sep[0], MavenArtifact.parse(sep[1])));
    }
    
    @Override
    public String toString(){
        return String.format("%s\t%s", hash, maven.map(MavenArtifact::toString).orElse(""));
    }
}
