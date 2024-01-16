package org.kettingpowered.launcher.dependency;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kettingpowered.launcher.KettingLauncher;
import org.kettingpowered.launcher.Main;
import org.kettingpowered.launcher.internal.utils.HashUtils;
import org.kettingpowered.launcher.internal.utils.NetworkUtils;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class Maven {
    public static @NotNull Path getPath(@NotNull String group, @NotNull String artifactId) {
        return Paths.get(group.replace('.','/'), artifactId);
    }
    public static Path getDependencyPath(Path path) {
        return Main.LauncherDir.toPath()
                .resolve(Main.INSTALLER_LIBRARIES_FOLDER)
                .resolve(path);
    }

    public static final List<String> hashAlgorithms = List.of(
            "SHA-512",
            "SHA-384",
            "SHA-256",
            "SHA-224",
            "SHA-1",
            "MD5",
            "MD2"
    );

    public static String hashAlgoToExt(String hashAlgorithm) {
        return switch (hashAlgorithm) {
            case "MD2" -> ".md2";
            case "MD5" -> ".md5";
            case "SHA-1" -> ".sha1";
            case "SHA-224" -> ".sha224";
            case "SHA-256" -> ".sha256";
            case "SHA-384" -> ".sha384";
            case "SHA-512", "SHA-512/224", "SHA-512/256" -> ".sha512";
            default -> hashAlgorithm;
        };
    }
    
    // Code below inspired from package dev.vankka.dependencydownload, but changed over time.
    // Check them out: https://github.com/Vankka/DependencyDownload
    public static boolean needsDownload(@Nullable Hash hash, final Path path, final MavenInfo info) throws NoSuchAlgorithmException, IOException {
        File dependencyFile = Maven.getDependencyPath(path).toFile().getAbsoluteFile();
        //noinspection ResultOfMethodCallIgnored
        dependencyFile.getParentFile().mkdirs();
        
        if (dependencyFile.exists()) {
            if (hash == null) {
                if (!dependencyFile.delete()) System.err.println("Something went wrong whilst deleting Dependency " + info);
                if (Main.DEBUG) System.out.println("Redownloading dep: "+info+" , because of an empty hash.");
                return true;
            }

            String fileHash = HashUtils.getHash(dependencyFile, hash.algorithm());
            if (fileHash.equals(hash.hash())) {
                if (Main.DEBUG) System.out.println("Dep Cached: "+info);
                // This dependency is already downloaded & the hash matches
                return false;
            } else {
                if (Main.DEBUG) System.out.println("Dep Hash-Mismatch. expected:" + hash + ", but got: " + fileHash + " redownloading: "+info);
                if (!dependencyFile.delete()) System.err.println("Something went wrong whilst deleting Dependency " + info);
                return true;
            }
        }
        if(Main.DEBUG) System.out.println("Dependency File does not exist: "+dependencyFile.getAbsolutePath());
        return true;
    }

    public static File downloadDependency(@Nullable final Hash hash, final MavenInfo maven) throws IOException, NoSuchAlgorithmException{
        return downloadDependency(hash, maven.getPath(), maven, false);
    }
    // Code below inspired from package dev.vankka.dependencydownload, but changed over time.
    // Check them out: https://github.com/Vankka/DependencyDownload
    static File downloadDependency(@Nullable final Hash hash, final Path path, final MavenInfo maven, boolean ignoreHashError) throws IOException, NoSuchAlgorithmException {
        final File dependencyFile = Maven.getDependencyPath(path).toFile();
        if (!needsDownload(hash, path, maven)) return dependencyFile;
        //noinspection ResultOfMethodCallIgnored
        dependencyFile.createNewFile();

        RuntimeException failure = new RuntimeException("All provided repositories failed to download dependency");
        if (Main.DEBUG) System.out.println("Downloading: "+maven);
        boolean anyFailures = false;
        for (String repository : AvailableMavenRepos.INSTANCE) {
            try {
                MessageDigest digest = hash!=null?MessageDigest.getInstance(hash.algorithm()):null;
                Maven.downloadFromRepository(repository, path, digest);

                @Nullable String gothash = HashUtils.getHash(digest);
                //we don't check the hash, if we were not passed one.
                //this is useful for downloading hashes itself
                if (gothash != null && !hash.hash().equals(gothash)) {
                    final String errorMessage = "Failed to verify file hash of Maven Artifact: " +maven+ "\nGot:      " + gothash + "\nExpected: " + hash.hash();
                    if (ignoreHashError) System.err.println(errorMessage);
                    else throw new RuntimeException(errorMessage);
                }

                // Success
                if (Main.DEBUG) System.out.println("Downloaded '" + maven + "' from "+repository);
                return dependencyFile;
            } catch (Throwable e) {
                //noinspection ResultOfMethodCallIgnored
                dependencyFile.delete();
                failure.addSuppressed(e);
                anyFailures = true;
            }
        }
        if (!anyFailures) {
            throw new RuntimeException("Nothing failed yet nothing passed");
        }
        throw failure;
    }
    
    static void downloadFromRepository(String repository, Path path, @Nullable MessageDigest digest) throws Throwable {
        if (!repository.endsWith("/")) repository = repository + "/";
        File dependencyFile = getDependencyPath(path).toFile().getAbsoluteFile();
        URLConnection connection = NetworkUtils.getConnection(repository+path);

        byte[] buffer = new byte[KettingLauncher.BufferSize];
        try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
            try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(dependencyFile))) {
                int total;
                while ((total = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, total);
                    if (digest!=null) digest.update(buffer, 0, total);
                }
            }
        }
    }
    //Code inspired by end

    public static File downloadDependency(Dependency<?> dep) throws IOException, NoSuchAlgorithmException {
        return downloadDependency(dep.hash(), dep.maven());
    }
}
