package org.kettingpowered.launcher.internal.utils;

import org.jetbrains.annotations.Nullable;
import org.kettingpowered.ketting.internal.KettingConstants;
import org.kettingpowered.launcher.lang.I18n;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//Inspired by https://git.magmafoundation.org/JustRed23/Magma-1-18-x/-/blob/1.18.x/magmalauncher/src/main/java/org/magmafoundation/magma/installer/NetworkUtils.java
//But changed a bit over time.
@SuppressWarnings("CallToPrintStackTrace")
public class NetworkUtils {

    private static final AtomicInteger threadNr = new AtomicInteger();
    private static final ExecutorService downloadSrvc  = java.util.concurrent.Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("network-utils-download-thread-"+threadNr.incrementAndGet());
        return thread;
    });


    public static URLConnection getConnection(String url) {
        URLConnection conn = null;
        try {
            conn = new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0");

            int timeout = (int) TimeUnit.SECONDS.toMillis(60);
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public static void downloadFile(String URL, File f) throws Exception {
        downloadFile(URL, f, null, "SHA-512");
    }

    public static void downloadFile(String URL, File f, @Nullable String expectedHash, String hashFormat) throws Exception {
        URLConnection conn = getConnection(URL);
        ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
        FileChannel fc = FileChannel.open(f.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        int fS = conn.getContentLength();
        CompletableFuture.supplyAsync(() -> {
            try {
                fc.transferFrom(rbc, 0, fS);
                fc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }, downloadSrvc).get(120, TimeUnit.SECONDS);
        rbc.close();
        String hash = HashUtils.getHash(f, hashFormat);
        if(expectedHash != null && !hash.equals(expectedHash.toLowerCase())) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
            throw new Exception(I18n.get("error.network_utils.hash_mismatch", hashFormat, expectedHash, hash));
        }
    }

    public static String readFileThrow(String URL) throws Exception {
        File f = File.createTempFile(KettingConstants.NAME, ".tmp");
        downloadFile(URL, f);
        return new String(Files.readAllBytes(f.toPath()));
    }
    public static String readFile(String URL) {
        try {
            return readFileThrow(URL);
        } catch (FileNotFoundException fnf) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
