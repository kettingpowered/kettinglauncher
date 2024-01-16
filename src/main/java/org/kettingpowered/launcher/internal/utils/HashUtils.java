package org.kettingpowered.launcher.internal.utils;


import org.jetbrains.annotations.Nullable;
import org.kettingpowered.launcher.KettingLauncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Code inspired by package dev.vankka.dependencydownload, Some stuff has maybe? changed.
// Check them out: https://github.com/Vankka/DependencyDownload
public class HashUtils {

    public static String getHash(File file, String algorithm) throws NoSuchAlgorithmException, IOException {
        try (FileInputStream stream = new FileInputStream(file)) {
            return getHash(stream, algorithm);
        }
    }

    public static String getHash(InputStream stream, String algorithm) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);

        byte[] buffer = new byte[KettingLauncher.BufferSize];
        int total;
        while ((total = stream.read(buffer)) != -1) {
            digest.update(buffer, 0, total);
        }

        return getHash(digest);
    }

    public static @Nullable String getHash(@Nullable MessageDigest digest) {
        if (digest==null) return null;
        StringBuilder result = new StringBuilder();
        for (byte b : digest.digest()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
