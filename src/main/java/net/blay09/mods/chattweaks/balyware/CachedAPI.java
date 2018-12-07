package net.blay09.mods.chattweaks.balyware;

import com.google.gson.stream.JsonReader;
import net.blay09.mods.chattweaks.ChatTweaks;
import net.minecraft.client.Minecraft;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import javax.annotation.Nullable;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class CachedAPI {

    private static final long DEFAULT_CACHE_TIME = 1000 * 60 * 60 * 24;

    public static Optional<JsonReader> loadCachedAPI(String url, String fileName, @Nullable String accept) {
        return loadCachedAPI(url, fileName, DEFAULT_CACHE_TIME, accept);
    }

    public static Optional<JsonReader> loadCachedAPI(String url, String fileName, long maxCacheTime, @Nullable String accept) {
        return loadCachedAPI(url, new File(getCacheDirectory(), fileName), maxCacheTime, accept);
    }

    public static Optional<JsonReader> loadCachedAPI(String url, File cacheFile, long maxCacheTime, @Nullable String accept) {
        //TODO: check if cache file is corrupted? (e.g. md5/crc32)
        if (!cacheFile.exists() || (System.currentTimeMillis() - cacheFile.lastModified() >= maxCacheTime)) {
            if (!loadRemote(url, accept, cacheFile)) {
                ChatTweaks.logger.error("failed to download new cache data, reading old cache data");
            }
        }

        try {
            ChatTweaks.logger.debug("reading cache file");
            return Optional.of(new JsonReader(new InputStreamReader(new FileInputStream(cacheFile), StandardCharsets.UTF_8)));
        } catch (FileNotFoundException e) {
            ChatTweaks.logger.error("cache file is missing: ", e);
        }

        return Optional.empty();
    }

    private static boolean loadRemote(String url, @Nullable String accept, File cacheFile) {
        ChatTweaks.logger.debug("downloading cache file");
        try {
            URL apiURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
            if (accept != null) {
                connection.setRequestProperty("Accept", accept);
            }
            connection.setRequestProperty("Accept-Encoding", "gzip");

            InputStream inputStream;
            if (connection.getContentEncoding().equals("gzip")) {
                inputStream = new GzipCompressorInputStream(connection.getInputStream());
            } else {
                inputStream = connection.getInputStream();
            }

            long bytes = Files.copy(inputStream, cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            ChatTweaks.logger.debug((float) (bytes * 1e-6) + " MB written");
            return true;
        } catch (UnknownHostException e) {
            ChatTweaks.logger.error("Can't connect to a Server, network connection may be interrupted: ", e);
            //TODO: check if network connection is even possible aka "minecraft client authenticated" or ping 8.8.8.8/1.1.1.1 --> differentiate between no network and DNS lookup failure?
        } catch (IOException e) {
            ChatTweaks.logger.error("An error occurred trying to load from an API: ", e);
        }

        return false;
    }

    public static File getCacheDirectory() {
        File file = new File(Minecraft.getMinecraft().mcDataDir, "chattweaks/cache/");
        if (!file.exists() && !file.mkdirs()) {
            throw new RuntimeException("Could not create cache directory for Chat Tweaks.");
        }
        return file;
    }

    public static File getImageCacheDirectory() {
        File file = new File(Minecraft.getMinecraft().mcDataDir, "chattweaks/cache/images");
        if (!file.exists() && !file.mkdirs()) {
            throw new RuntimeException("Could not create image cache directory for Chat Tweaks.");
        }
        return file;
    }

    public static boolean invalidateCacheFile(File cacheFile) {
        ChatTweaks.logger.debug("invalidating cached file: " + cacheFile.getPath());
        return cacheFile.setLastModified(cacheFile.lastModified() - DEFAULT_CACHE_TIME * 2);
    }
}
