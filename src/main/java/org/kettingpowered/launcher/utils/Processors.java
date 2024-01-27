package org.kettingpowered.launcher.utils;

import org.kettingpowered.launcher.lang.I18n;

import java.lang.reflect.InvocationTargetException;

public final class Processors {

    public static void execute(ClassLoader cl, String processor, String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        processor = processor.split(":")[1];

        switch (processor) {
            case "binarypatcher":
                Class.forName("net.minecraftforge.binarypatcher.ConsoleTool", true, cl)
                        .getDeclaredMethod("main", String[].class)
                        .invoke(null, (Object) args);
                break;
            case "installertools":
                Class.forName("net.minecraftforge.installertools.ConsoleTool", true, cl)
                        .getDeclaredMethod("main", String[].class)
                        .invoke(null, (Object) args);
                break;
            case "jarsplitter":
                Class.forName("net.minecraftforge.jarsplitter.ConsoleTool", true, cl)
                        .getDeclaredMethod("main", String[].class)
                        .invoke(null, (Object) args);
                break;
            case "ForgeAutoRenamingTool":
                Class.forName("net.minecraftforge.fart.Main", true, cl)
                        .getDeclaredMethod("main", String[].class)
                        .invoke(null, (Object) args);
                break;
            default:
                throw new IllegalArgumentException(I18n.get("error.processor.unknown_processor", processor));
        }
    }
}
