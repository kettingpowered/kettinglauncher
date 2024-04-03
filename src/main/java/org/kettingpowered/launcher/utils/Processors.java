package org.kettingpowered.launcher.utils;

import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.ketting.internal.Type;
import org.kettingpowered.launcher.lang.I18n;

import java.lang.reflect.InvocationTargetException;

public final class Processors {

    public static void execute(ClassLoader cl, String processor, String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        processor = processor.split(":")[1];

        String packageName;
        if (KettingConstants.TYPE == Type.Forge) {
            packageName = "net.minecraftforge.";
        } else if (KettingConstants.TYPE == Type.NeoForge) {
            packageName = "net.neoforged.";
        } else{
            throw new RuntimeException("Unsupported Type");
        }
        
        switch (processor) {
            case "binarypatcher":
                Class.forName(packageName + "binarypatcher.ConsoleTool", true, cl)
                        .getDeclaredMethod("main", String[].class)
                        .invoke(null, (Object) args);
                break;
            case "installertools":
                Class.forName(packageName + "installertools.ConsoleTool", true, cl)
                        .getDeclaredMethod("main", String[].class)
                        .invoke(null, (Object) args);
                break;
            case "jarsplitter":
                Class.forName(packageName + "jarsplitter.ConsoleTool", true, cl)
                        .getDeclaredMethod("main", String[].class)
                        .invoke(null, (Object) args);
                break;
            case "ForgeAutoRenamingTool", "AutoRenamingTool":
                Class.forName("net.minecraftforge.fart.Main", true, cl)
                        .getDeclaredMethod("main", String[].class)
                        .invoke(null, (Object) args);
                break;
            default:
                throw new IllegalArgumentException(I18n.get("error.processor.unknown_processor", processor));
        }
    }
}
