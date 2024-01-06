package org.kettingpowered.launcher.dependency;

import org.jetbrains.annotations.NotNull;
import java.util.Optional;

/**
 *
 * @param hash a hash, of the downloaded file, with the hash-type below
 * @param hashType hash-type of the hash, conforming to <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#messagedigest-algorithms">Java Standard Hashing Algorithm Names</a>
 * @param maven maven artifact location
 */
public record Dependency(String hash, String hashType, Optional<MavenArtifact> maven) {
    public static @NotNull Optional<Dependency> parse(String line){
        final String[] sep = line.trim().split("\t");
        if (sep.length < 3) return Optional.empty();
        return Optional.of(new Dependency(sep[0], sep[1], MavenArtifact.parse(sep[2])));
    }
    
    @Override
    public String toString(){
        return String.format("%s\t%s", hash, maven.map(MavenArtifact::toString).orElse(""));
    }
}
