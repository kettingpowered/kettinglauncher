package org.kettingpowered.launcher.dependency;

import org.jetbrains.annotations.NotNull;
import org.kettingpowered.ketting.internal.MajorMinorPatchVersion;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Parses a string with the format of 'group:artifact:version(:classifer)?(@extenstion)?'
 * the group should not contain to consecutive '.' or a '.' at the very beginning/end.
 * Breaking that may lead to wrong artifact resolution.
 * @author C0D3 M4513R
 */
public record MavenArtifact(String group, String artifactId, String version, Optional<String> classifier, Optional<String> extension)
implements MavenInfo {
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

    public Path getPath() {
        return Maven.getPath(group, artifactId)
                .resolve(version)
                .resolve(getFileNameWithExtenstion());
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

    public MavenArtifact withVersion(String version){
        return new MavenArtifact(group, artifactId, version, classifier, extension);
    }

    public MavenManifest asManifest(){
        return new MavenManifest(group, artifactId);
    }

    public MavenArtifact getLatestMinorPatch() throws Exception {
        MajorMinorPatchVersion<String> version = MajorMinorPatchVersion.parse(version());
        List<String> versions = asManifest()
                .getDepVersions()
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

    public File download() throws Exception {
        return Maven.downloadDependency(downloadDependencyHash());
    }
    @Override
    public Dependency<MavenArtifact> downloadDependencyHash() throws Exception{
        return MavenInfo.downloadDependencyHash(this);
    }
    //Code inspiration end
}
