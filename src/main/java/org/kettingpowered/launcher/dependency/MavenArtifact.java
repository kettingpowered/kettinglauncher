package org.kettingpowered.launcher.dependency;

import org.jetbrains.annotations.NotNull;
import org.kettingpowered.ketting.internal.MajorMinorPatchVersion;
import org.kettingpowered.ketting.internal.Tuple;
import org.kettingpowered.launcher.KettingLauncher;
import org.kettingpowered.launcher.Main;
import org.kettingpowered.launcher.internal.utils.NetworkUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses a string with the format of 'group:artifact:version(:classifer)?(@extenstion)?'
 * the group should not contain to consecutive '.' or a '.' at the very beginning/end.
 * Breaking that may lead to wrong artifact resolution.
 * @author C0D3 M4513R
 */
public record MavenArtifact(String group, String artifactId, String version, Optional<String> classifier, Optional<String> extension) {
    public static @NotNull Optional<MavenArtifact> parse(String parse){
        final String[] sep = parse.trim().split(":");
        String last = sep[sep.length-1];
        Optional<String> extension = Optional.empty();
        final int extSep = last.indexOf('@');
        if (extSep>=0){
            extension = Optional.of(last.substring(extSep+1));
            sep[sep.length-1] = last.substring(0,extSep);
        }
        if (sep.length<3) return Optional.empty();
        return Optional.of(new MavenArtifact(sep[0], sep[1], sep[2], sep.length>=4?Optional.of(sep[3]):Optional.empty(), extension));
    }

    public String getPath() {
        return getPath(group, artifactId) + "/" + version + "/" + getFileNameWithExtenstion() ;
    }

    public String getFileName() {
        String name = artifactId+"-"+version;
        if (classifier.isPresent()) {
            name += "-" + classifier.get();
        }
        return name;
    }

    public String getFileNameWithExtenstion() {
        String name = getFileName();
        if (extension.isPresent()) {
            name += "."+extension.get();
        }
        return name;
    }

    public boolean equalsIgnoringVersion(MavenArtifact artifact){
        return group.equals(artifact.group) && artifactId.equals(artifact.artifactId) &&
                (
                    (classifier.isEmpty() && artifact.classifier.isEmpty()) ||
                    classifier.isPresent() && artifact.classifier.isPresent() &&
                    classifier.get().equals(artifact.classifier.get())
                ) && (
                    (extension.isEmpty() && artifact.extension.isEmpty()) ||
                    extension.isPresent() && artifact.extension.isPresent() &&
                    extension.get().equals(artifact.extension.get())
                );
    }
    
    
    @Override
    public String toString(){
        String gradleId = group+":"+artifactId+":"+version;
        if (classifier.isPresent()){
            gradleId += ":"+classifier.get();
        }
        if (extension.isPresent()) {
            gradleId += "@"+extension.get();
        }
        return gradleId;
    }
    
    public static @NotNull String getPath(@NotNull String group, @NotNull String artifactId) {
        return group.replace('.','/') + "/" + artifactId;
    }

    public MavenArtifact withVersion(String version){
        return new MavenArtifact(group, artifactId, version, classifier, extension);
    }

    public List<String> getDepVersions() throws Exception {
        return getDepVersions(group, artifactId);
    }
    
    public static List<String> getDepVersions(@NotNull String group, @NotNull String artifactId) throws Exception {
        final File lib = MavenArtifact.downloadMavenMetadata(group, artifactId);
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(lib));
        final List<String> list = new ArrayList<>();

        final NodeList versions = doc.getElementsByTagName("version");
        if (versions.getLength() < 1) throw new Exception("Maven Metadata contains no 'versions' Tag");
        int i = 0;
        while(i < versions.getLength()) {
            list.add(versions.item(i).getFirstChild().getNodeValue());
            i++;
        }
        if (Main.DEBUG) System.out.println(String.join("\n", list));
        return new ArrayList<>(list);
    }

    public MavenArtifact getLatestMinorPatch() throws Exception {
        MajorMinorPatchVersion<String> version = MajorMinorPatchVersion.parse(version());
        List<String> versions = getDepVersions()
                .stream()
                .map(MajorMinorPatchVersion::parse)
                //restrict versions to only update to latest minor change.
                //Major changes indicate a BREAKING change.
                .filter(mmp -> mmp.major().equals(version.major()))
                .sorted()
                .map(MajorMinorPatchVersion::toString).toList();
        if (!versions.isEmpty()) return withVersion(versions.get(versions.size()-1));
        return this;
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

    public Dependency downloadDependencyHash() throws Exception{
        Tuple<String, String> deps = downloadDependencyHash(getPath());
        return new Dependency(deps.t1(), deps.t2(), Optional.of(this));
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
    
    public File downloadDependencyAndHash() throws Exception {
        return downloadDependencyAndHash(getPath());
    }
    public static File downloadMavenMetadata(String group, String artifactId) throws Exception {
        return downloadDependencyAndHash(getPath(group, artifactId) + "/maven-metadata.xml");
    }
    
    // Code below inspired from package dev.vankka.dependencydownload, but changed over time.
    // Check them out: https://github.com/Vankka/DependencyDownload
    public static File downloadDependencyAndHash(String path) throws Exception {
        Instant start = Instant.now();
        File file = new File(new File(Main.LauncherDir, Main.INSTALLER_LIBRARIES_FOLDER), path);
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        Exception exception = new Exception("Failed to get hash for "+path+" from all repos");
        boolean downloaded = false;
        for(final String repo:AvailableMavenRepos.INSTANCE){
            Exception exception1 = new Exception("Failed to any hash for "+path+" from repo: "+ repo);
            for(final String hashAlgorithm:MavenArtifact.hashAlgorithms){
                try{
                    String hash = NetworkUtils.readFileThrow(repo+path+MavenArtifact.hashAlgoToExt(hashAlgorithm));
                    NetworkUtils.downloadFile(repo+path, file, hash, hashAlgorithm);
                    downloaded = true;
                }catch (Throwable throwable){
                    exception1.addSuppressed(throwable);
                }
            }
            if (!downloaded) exception.addSuppressed(exception1);
        }
        if (Main.DEBUG) System.out.println("downloadDependencyAndHash took:"+ Duration.between(start, Instant.now()));
        if (!downloaded) throw exception;
        return file;
    }

    void downloadFromRepository(String repository, File dependencyFile, MessageDigest digest) throws Throwable {
        if (!repository.endsWith("/")) repository = repository + "/";
        String path = getPath();
        if (path.startsWith("/")) path = path.substring(1);
        URLConnection connection = NetworkUtils.getConnection(repository+path);

        byte[] buffer = new byte[KettingLauncher.BufferSize];
        try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
            try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(dependencyFile))) {
                int total;
                while ((total = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, total);
                    digest.update(buffer, 0, total);
                }
            }
        }
    }
    public Path getDependencyPath() {
        return Main.LauncherDir.toPath()
                .resolve(Main.INSTALLER_LIBRARIES_FOLDER)
                .resolve(getPath());
    }
    //Code inspiration end
}
