package org.kettingpowered.launcher.dependency;

import org.jetbrains.annotations.NotNull;
import org.kettingpowered.launcher.internal.utils.NetworkUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses a string with the format of 'group:artifact:version(:classifer)?(@extenstion)?'
 * the group should not contain to consecutive '.' or a '.' at the very beginning/end.
 * Breaking that may lead to wrong artifact resolution.
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
    
    public static List<String> getDepVersions(@NotNull String group, @NotNull String artifactId) throws Exception {
        String hash = null, content = null;
        boolean downloaded = false;
        Exception exception = new Exception("Failed to get metadata from all repositories");
        for(String repo: AvailableMavenRepos.INSTANCE){
            try{
                final String url = repo + getPath(group, artifactId) + "/maven-metadata.xml";
                hash = NetworkUtils.downloadToString(url + ".sha512", null, null);
                content = NetworkUtils.downloadToString(url, hash, "SHA-512");
                downloaded = true;
                break;
            }catch (Throwable throwable){
                exception.addSuppressed(throwable);
            }
        }
        if (!downloaded){
            throw exception;
        }
        assert hash != null && content != null;
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(content);
        final List<String> list = new ArrayList<>();

        final NodeList nl = doc.getElementsByTagName("versions");
        if (nl.getLength() > 1) throw new Exception("Maven Metadata contains more than one 'versions' Tag");
        if (nl.getLength() < 1) throw new Exception("Maven Metadata contains no 'versions' Tag");
        final NodeList versions = nl.item(1).getChildNodes();
        int i = 0;
        while(i < versions.getLength()) list.add(versions.item(i).getNodeValue());
        return new ArrayList<>(list);
    }
}
