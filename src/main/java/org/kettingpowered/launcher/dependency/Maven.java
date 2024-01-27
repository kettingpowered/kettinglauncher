package org.kettingpowered.launcher.dependency;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kettingpowered.launcher.KettingLauncher;
import org.kettingpowered.launcher.Main;
import org.kettingpowered.launcher.internal.utils.HashUtils;
import org.kettingpowered.launcher.internal.utils.NetworkUtils;
import org.kettingpowered.launcher.lang.I18n;
import org.kettingpowered.launcher.log.LogLevel;
import org.kettingpowered.launcher.log.Logger;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
                if (!dependencyFile.delete()) I18n.logError("error.maven.failed_to_delete_dep", info);
                I18n.logDebug("debug.maven.re_downloading.empty_hash", info);
                return true;
            }

            String fileHash = HashUtils.getHash(dependencyFile, hash.algorithm());
            if (fileHash.equals(hash.hash())) {
                I18n.logDebug("debug.maven.dep_hash_match", info);
                // This dependency is already downloaded & the hash matches
                return false;
            } else {
                I18n.logDebug("debug.maven.re_downloading.hash_mismatch", info, hash, fileHash);
                if (!dependencyFile.delete()) I18n.logError("error.maven.failed_to_delete_dep", info);
                return true;
            }
        }
        I18n.logDebug("debug.maven.dep_not_found", dependencyFile.getAbsolutePath());
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

        RuntimeException failure = new RuntimeException(I18n.get("error.maven.all_repos_failed"));
        I18n.logDebug("debug.maven.downloading_dep", maven);
        boolean anyFailures = false;
        for (String repository : AvailableMavenRepos.INSTANCE) {
            try {
                MessageDigest digest = hash!=null?MessageDigest.getInstance(hash.algorithm()):null;
                Maven.downloadFromRepository(repository, path, digest);

                @Nullable String gothash = HashUtils.getHash(digest);
                //we don't check the hash, if we were not passed one.
                //this is useful for downloading hashes itself
                if (gothash != null && !hash.hash().equals(gothash)) {
                    final String errorMessage = I18n.get("error.maven.hash_mismatch", maven, gothash, hash.hash());
                    if (ignoreHashError) Logger.log(LogLevel.ERROR, errorMessage);
                    else throw new RuntimeException(errorMessage);
                }

                // Success
                I18n.logDebug("debug.maven.downloaded_dep", maven, repository);
                return dependencyFile;
            } catch (Throwable e) {
                //noinspection ResultOfMethodCallIgnored
                dependencyFile.delete();
                failure.addSuppressed(e);
                anyFailures = true;
            }
        }
        if (!anyFailures) {
            throw new RuntimeException(I18n.get("error.maven.unknown_error"));
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
