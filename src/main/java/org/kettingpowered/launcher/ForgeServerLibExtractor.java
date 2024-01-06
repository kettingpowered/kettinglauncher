package org.kettingpowered.launcher;

import org.kettingpowered.ketting.internal.KettingFileVersioned;
import org.kettingpowered.ketting.internal.KettingFiles;
import org.kettingpowered.launcher.internal.utils.JarTool;
import java.io.IOException;

public class ForgeServerLibExtractor {
    public static void extract() throws IOException {
        JarTool.extractJarContent(KettingFiles.DATA_DIR + KettingFileVersioned.FORGE_UNIVERSAL_NAME, KettingFileVersioned.FORGE_UNIVERSAL_JAR);
        JarTool.extractJarContent(KettingFiles.DATA_DIR + KettingFileVersioned.FMLCORE_NAME, KettingFileVersioned.FMLCORE);
        JarTool.extractJarContent(KettingFiles.DATA_DIR + KettingFileVersioned.FMLLOADER_NAME, KettingFileVersioned.FMLLOADER);
        JarTool.extractJarContent(KettingFiles.DATA_DIR + KettingFileVersioned.JAVAFMLLANGUAGE_NAME, KettingFileVersioned.JAVAFMLLANGUAGE);
        JarTool.extractJarContent(KettingFiles.DATA_DIR + KettingFileVersioned.LOWCODELANGUAGE_NAME, KettingFileVersioned.LOWCODELANGUAGE);
        JarTool.extractJarContent(KettingFiles.DATA_DIR + KettingFileVersioned.MCLANGUAGE_NAME, KettingFileVersioned.MCLANGUAGE);
    }
}
