package org.kettingpowered.launcher.dependency;

import org.jetbrains.annotations.NotNull;
import org.kettingpowered.launcher.Main;
import org.kettingpowered.launcher.internal.utils.Hash;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 *
 * @param hash a hash, of the downloaded file, with the hash-type below
 * @param hashType hash-type of the hash, conforming to <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#messagedigest-algorithms">Java Standard Hashing Algorithm Names</a>
 * @param maven maven artifact location
 * @author C0D3 M4513R
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

    // Code below inspired from package dev.vankka.dependencydownload, but changed over time.
    // Check them out: https://github.com/Vankka/DependencyDownload
    public void downloadDependency() throws IOException, NoSuchAlgorithmException {
        if (maven.isEmpty()) throw new IllegalArgumentException("Passed dependency has no maven information");
        MavenArtifact maven = this.maven.get();
        Path dependencyPath = maven.getDependencyPath();

        if (!Files.exists(dependencyPath.getParent())) {
            Files.createDirectories(dependencyPath.getParent());
        }

        if (Files.exists(dependencyPath)) {
            String fileHash = Hash.getHash(dependencyPath.toFile(), hashType);
            if (fileHash.equals(hash)) {
                if (Main.DEBUG) System.out.println("Dep Cached: "+maven);
                // This dependency is already downloaded & the hash matches
                return;
            } else {
                if (Main.DEBUG) System.out.println("Dep Hash-Mismatch. expected:" + hash + ", but got: " + fileHash + " redownloading: "+maven);
                Files.delete(dependencyPath);
            }
        }
        Files.createFile(dependencyPath);

        RuntimeException failure = new RuntimeException("All provided repositories failed to download dependency");
        if (Main.DEBUG) System.out.println("Downloading: "+maven);
        boolean anyFailures = false;
        for (String repository : AvailableMavenRepos.INSTANCE) {
            try {
                MessageDigest digest = MessageDigest.getInstance(hashType);
                maven.downloadFromRepository(repository, dependencyPath, digest);

                String hash = Hash.getHash(digest);
                if (!this.hash.equals(hash)) {
                    throw new RuntimeException("Failed to verify file hash: " + hash + " should've been: " + hash);
                }

                // Success
                if (Main.DEBUG) System.out.println("Downloaded '" + maven + "' from "+repository);
                return;
            } catch (Throwable e) {
                Files.deleteIfExists(dependencyPath);
                failure.addSuppressed(e);
                anyFailures = true;
            }
        }
        if (!anyFailures) {
            throw new RuntimeException("Nothing failed yet nothing passed");
        }
        throw failure;
    }
    //Code inspired by end
}
