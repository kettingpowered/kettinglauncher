package org.kettingpowered.launcher.dependency;

import org.jetbrains.annotations.NotNull;
import org.kettingpowered.launcher.Main;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
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
        final String path = getPath(group, artifactId) + "/maven-metadata.xml";
        final File lib = LibHelper.downloadDependencyAndHash(path);
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
}
