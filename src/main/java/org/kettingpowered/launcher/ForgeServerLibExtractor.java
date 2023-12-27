package org.kettingpowered.launcher;

import org.kettingpowered.launcher.internal.utils.JarTool;
import java.io.IOException;

public class ForgeServerLibExtractor {
    public static void extract() throws IOException {
        JarTool.extractJarContent(KettingFiles.DATA_DIR + KettingFiles.FORGE_UNIVERSAL_NAME, KettingFiles.FORGE_UNIVERSAL_JAR);
        JarTool.extractJarContent(KettingFiles.DATA_DIR + KettingFiles.FMLCORE_NAME, KettingFiles.FMLCORE);
        JarTool.extractJarContent(KettingFiles.DATA_DIR + KettingFiles.FMLLOADER_NAME, KettingFiles.FMLLOADER);
        JarTool.extractJarContent(KettingFiles.DATA_DIR + KettingFiles.JAVAFMLLANGUAGE_NAME, KettingFiles.JAVAFMLLANGUAGE);
        JarTool.extractJarContent(KettingFiles.DATA_DIR + KettingFiles.LOWCODELANGUAGE_NAME, KettingFiles.LOWCODELANGUAGE);
        JarTool.extractJarContent(KettingFiles.DATA_DIR + KettingFiles.MCLANGUAGE_NAME, KettingFiles.MCLANGUAGE);
    }
}
