package org.kettingpowered.launcher.dependency;

import org.kettingpowered.ketting.internal.KettingFiles;
import org.kettingpowered.ketting.internal.Tuple;
import org.kettingpowered.launcher.KettingLauncher;
import org.kettingpowered.launcher.Main;
import org.kettingpowered.launcher.internal.utils.Hash;
import org.kettingpowered.launcher.internal.utils.NetworkUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;


// Code inspired from package dev.vankka.dependencydownload, but changed over time.
// Check them out: https://github.com/Vankka/DependencyDownload
public final class LibHelper {
    public static final Path baseDirPath = KettingLauncher.LauncherDir.toPath();
    
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
    
    public static Dependency downloadDependencyHash(final MavenArtifact artifact) throws Exception{
        Tuple<String, String> deps = downloadDependencyHash(artifact.getPath());
        return new Dependency(deps.t1(), deps.t2(), Optional.of(artifact));
    }

    /**
     * Gets a hash of a path
     * @param path path 
     * @return tuple containing the hash, and the MessageDigest Name of the Hashing-Function
     * @throws Exception if no hash could be found
     */
    public static Tuple<String, String> downloadDependencyHash(final String path) throws Exception{
        String hash = null;
        String hashAlgorithmOut = null;
        Exception exception = new Exception("Failed to get hash for "+path+" from all repos");
        boolean downloaded = false;
        for(final String repo:AvailableMavenRepos.INSTANCE){
            Exception exception1 = new Exception("Failed to any hash for "+path+" from repo: "+ repo);
            for(final String hashAlgorithm:hashAlgorithms){
                try{
                    hash = NetworkUtils.readFileThrow(repo+path+hashAlgoToExt(hashAlgorithm));
                    hashAlgorithmOut = hashAlgorithm;
                    downloaded = true;
                }catch (Throwable throwable){
                    exception1.addSuppressed(throwable);
                }
            }
            if (!downloaded) exception.addSuppressed(exception1);
        }
        if (!downloaded) throw exception;
        return new Tuple<>(hash, hashAlgorithmOut);
    }
    
    
    public static File downloadDependencyAndHash(String path) throws Exception {
        File file = new File(KettingFiles.LIBRARIES_DIR, path);
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        Exception exception = new Exception("Failed to get hash for "+path+" from all repos");
        boolean downloaded = false;
        for(final String repo:AvailableMavenRepos.INSTANCE){
            Exception exception1 = new Exception("Failed to any hash for "+path+" from repo: "+ repo);
            for(final String hashAlgorithm:hashAlgorithms){
                try{
                    String hash = NetworkUtils.readFileThrow(repo+path+hashAlgoToExt(hashAlgorithm));
                    NetworkUtils.downloadFile(repo+path, file, hash, hashAlgorithm);
                    downloaded = true;
                }catch (Throwable throwable){
                    exception1.addSuppressed(throwable);
                }
            }
            if (!downloaded) exception.addSuppressed(exception1);
        }
        if (!downloaded) throw exception;
        return file;
    }

    public static void downloadDependency(Dependency dependency) throws IOException, NoSuchAlgorithmException {
        if (dependency.maven().isEmpty()) throw new IllegalArgumentException("Passed dependency has no maven information");
        Path dependencyPath = getDependencyPath(dependency);

        if (!Files.exists(dependencyPath.getParent())) {
            Files.createDirectories(dependencyPath.getParent());
        }

        if (Files.exists(dependencyPath)) {
            String fileHash = Hash.getHash(dependencyPath.toFile(), dependency.hashType());
            if (fileHash.equals(dependency.hash())) {
                if (Main.DEBUG) System.out.println("Dep Cached: "+dependency.maven().get());
                // This dependency is already downloaded & the hash matches
                return;
            } else {
                if (Main.DEBUG) System.out.println("Dep Hash-Mismatch. expected:" + dependency.hash() + ", but got: " + fileHash + " redownloading: "+dependency.maven().get());
                Files.delete(dependencyPath);
            }
        }
        Files.createFile(dependencyPath);

        RuntimeException failure = new RuntimeException("All provided repositories failed to download dependency");
        if (Main.DEBUG) System.out.println("Downloading: "+dependency.maven().get());
        boolean anyFailures = false;
        for (String repository : AvailableMavenRepos.INSTANCE) {
            try {
                MessageDigest digest = MessageDigest.getInstance(dependency.hashType());
                downloadFromRepository(dependency, repository, dependencyPath, digest);

                String hash = Hash.getHash(digest);
                String dependencyHash = dependency.hash();
                if (!hash.equals(dependencyHash)) {
                    throw new RuntimeException("Failed to verify file hash: " + hash + " should've been: " + dependencyHash);
                }

                // Success
                if (Main.DEBUG) System.out.println("Downloaded '" + dependency.maven().get() + "' from "+repository);
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
        if (dependency.maven().isEmpty()) throw new IllegalArgumentException("Passed dependency has no maven information");
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
        if (dependency.maven().isEmpty()) throw new IllegalArgumentException("Passed dependency has no maven information");
        MavenArtifact maven = dependency.maven().get(); 
        return baseDirPath.resolve("libraries")
                .resolve(maven.getPath());
    }
}
