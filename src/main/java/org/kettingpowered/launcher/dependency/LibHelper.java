package org.kettingpowered.launcher.dependency;

import org.kettingpowered.launcher.KettingLauncher;
import org.kettingpowered.launcher.internal.utils.Hash;
import org.kettingpowered.launcher.internal.utils.JarTool;
import org.kettingpowered.launcher.internal.utils.NetworkUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


// Code inspired from package dev.vankka.dependencydownload.
// Check them out: https://github.com/Vankka/DependencyDownload
public final class LibHelper {
    public static final Path baseDirPath = JarTool.getJarDir().toPath();

    public static void downloadDependency(Dependency dependency) throws IOException, NoSuchAlgorithmException {
        Path dependencyPath = getDependencyPath(dependency);

        if (!Files.exists(dependencyPath.getParent())) {
            Files.createDirectories(dependencyPath.getParent());
        }

        if (Files.exists(dependencyPath)) {
            String fileHash = Hash.getHash(dependencyPath.toFile(), "SHA-256");
            if (fileHash.equals(dependency.hash())) {
                // This dependency is already downloaded & the hash matches
                return;
            } else {
                Files.delete(dependencyPath);
            }
        }
        Files.createFile(dependencyPath);

        RuntimeException failure = new RuntimeException("All provided repositories failed to download dependency");
        boolean anyFailures = false;
        for (String repository : AvailableMavenRepos.INSTANCE) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                downloadFromRepository(dependency, repository, dependencyPath, digest);

                String hash = Hash.getHash(digest);
                String dependencyHash = dependency.hash();
                if (!hash.equals(dependencyHash)) {
                    throw new RuntimeException("Failed to verify file hash: " + hash + " should've been: " + dependencyHash);
                }

                // Success
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

    private static void downloadFromRepository(Dependency dependency, String repository, Path dependencyPath, MessageDigest digest) throws Throwable {
        if (!repository.endsWith("/")) repository = repository + "/";
        String path = dependency.maven().get().getPath();
        if (path.startsWith("/")) path = path.substring(1);
        URLConnection connection = NetworkUtils.getConnection(repository+path);

        byte[] buffer = new byte[KettingLauncher.BufferSize];
        try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
            try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(dependencyPath))) {
                int total;
                while ((total = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, total);
                    digest.update(buffer, 0, total);
                }
            }
        }
    }
    public static Path getDependencyPath(Dependency dependency) {
        MavenArtifact maven = dependency.maven().get(); 
        return baseDirPath.resolve("libraries")
                .resolve(maven.getPath());
    }
}
