package org.kettingpowered.launcher.dependency;

import org.kettingpowered.launcher.KettingLauncher;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

public class LibraryClassLoader extends URLClassLoader {
    public LibraryClassLoader(URL[] libs, ClassLoader parent) {
        super(libs, parent);
    }

    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        System.out.println("findClass for "+name);
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            String name2 = name.replace(".", "/");
            if (Arrays.stream(KettingLauncher.MANUALLY_PATCHED_LIBS).anyMatch(name2::startsWith))
                throw e; //ignore errors for manually patched libs

            return ClassLoader.getSystemClassLoader().loadClass(name);
        }
    }
}
