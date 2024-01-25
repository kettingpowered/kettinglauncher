package org.kettingpowered.launcher.dependency;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 *
 * @param hash a hash, of the downloaded file, with the hash-type below
 * @param maven maven artifact location
 * @author C0D3 M4513R
 */
public record Dependency<T extends MavenInfo>(Hash hash, T maven)
implements MavenInfo {
    public static @NotNull Optional<Dependency<MavenArtifact>> parse(String line){
        final String[] sep = line.trim().split("\t");
        if (sep.length < 3) return Optional.empty();
        Optional<MavenArtifact> mavenArtifact = MavenArtifact.parse(sep[2]);
        return mavenArtifact.map(artifact -> new Dependency<>(new Hash(sep[0], sep[1]), artifact));
    }
    
    @Override
    public String toString(){
        return String.format("%s\t%s", hash, maven.toString());
    }

    @Override
    public Path getPath() {
        return maven.getPath();
    }

    @Override
    public String getFileName() {
        return maven.getFileName();
    }

    @Override
    public String getFileNameWithExtenstion() {
        return maven.getFileNameWithExtenstion();
    }
    
    public File download() throws IOException, NoSuchAlgorithmException {
        return Maven.downloadDependency(this);
    }

    @Override
    public Dependency<T> downloadDependencyHash() {
        return this;
    }
}
