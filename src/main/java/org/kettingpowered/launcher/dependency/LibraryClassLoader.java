package org.kettingpowered.launcher.dependency;

import org.kettingpowered.launcher.KettingLauncher;

import java.net.URL;
import java.net.URLClassLoader;

public class LibraryClassLoader extends URLClassLoader {
    public LibraryClassLoader(URL[] libs){
        super(libs);
    }

    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            String name2 = name.replace(".", "/");
            if (KettingLauncher.MANUALLY_PATCHED_LIBS.stream().anyMatch(name2::startsWith))
                throw e; //ignore errors for manually patched libs

            return ClassLoader.getSystemClassLoader().loadClass(name);
        }
    }
}
