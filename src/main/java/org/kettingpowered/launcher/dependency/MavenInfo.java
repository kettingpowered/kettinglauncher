package org.kettingpowered.launcher.dependency;

import org.kettingpowered.launcher.Main;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public interface MavenInfo {
    Path getPath();
    String getFileName();
    String getFileNameWithExtenstion();
    File download() throws Exception;
    Dependency<? extends MavenInfo> downloadDependencyHash() throws Exception;
    static <T extends MavenInfo> Dependency<T> downloadDependencyHash(T mavenInfo) throws Exception{
        if (Main.DEBUG) System.out.println("Downloading hash for: "+ mavenInfo);
        Hash deps = null;
        Path path = Objects.requireNonNull(mavenInfo.getPath().getParent());
        String fileName = mavenInfo.getFileNameWithExtenstion();
        Exception exception = new Exception("Failed to download any hash");
        for (String hashAlgorithm:Maven.hashAlgorithms){
            try{
                String hashExt = Maven.hashAlgoToExt(hashAlgorithm);
                deps = new Hash(
                        Files.readString(
                                Maven.downloadDependency(
                                        null,
                                        path.resolve(fileName+hashExt),
                                        mavenInfo,
                                        true
                                ).toPath()
                                .toAbsolutePath()
                        ),
                        hashAlgorithm
                );
                break;
            }catch (Throwable throwable){
                exception.addSuppressed(throwable);
            }
        }
        if (deps==null) throw exception;
        if (Main.DEBUG) System.out.println("Downloaded  hash for: "+ mavenInfo);
        return new Dependency<>(deps, mavenInfo);
    }
}
