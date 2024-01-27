package org.kettingpowered.launcher.dependency;

import org.kettingpowered.launcher.lang.I18n;
import org.kettingpowered.launcher.log.LogLevel;
import org.kettingpowered.launcher.log.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record MavenManifest(String group, String artifactId)
implements MavenInfo {
    public List<String> getDepVersions() throws Exception {
        final File lib = download();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(lib));
        final List<String> list = new ArrayList<>();

        final NodeList versions = doc.getElementsByTagName("version");
        if (versions.getLength() < 1) throw new Exception(I18n.get("error.maven.no_versions"));
        int i = 0;
        while(i < versions.getLength()) {
            list.add(versions.item(i).getFirstChild().getNodeValue());
            i++;
        }
        Logger.log(LogLevel.DEBUG, String.join("\n", list));
        return new ArrayList<>(list);
    }

    public Path getPath() {
        return Maven.getPath(group, artifactId)
                .resolve(getFileNameWithExtenstion());
    }

    public String getFileName() {
        return "maven-metadata";
    }

    public String getFileNameWithExtenstion() {
        return getFileName()+".xml";
    }

    @Override
    public File download() throws Exception {
        return Maven.downloadDependency(downloadDependencyHash().hash(), this.getPath(), this, true);
    }

    @Override
    public Dependency<MavenManifest> downloadDependencyHash() throws Exception{
        return MavenInfo.downloadDependencyHash(this);
    }
}
