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
        //TODO: check if cache file is corrupted (e.g. SHA)
        if (!cacheFile.exists() || (System.currentTimeMillis() - cacheFile.lastModified() >= maxCacheTime)) {
            if (!loadRemote(url, accept, cacheFile)) {
                ChatTweaks.logger.warn("failed to download new data, reading old cache file");
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
        ChatTweaks.logger.debug("downloading file");
        try {
            URL apiURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
            if (accept != null) {
                connection.setRequestProperty("Accept", accept);
            }
            connection.setRequestProperty("Accept-Encoding", "gzip");

            String encoding = connection.getContentEncoding();
            ChatTweaks.logger.debug("Encoding: " + encoding);

            InputStream inputStream;
            if (encoding.equals("gzip"))
            {
                inputStream = new GzipCompressorInputStream(connection.getInputStream());
            }
            else
            {
                inputStream = connection.getInputStream();
            }

            long bytes = Files.copy(inputStream, cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            ChatTweaks.logger.debug((float)(bytes * 1e-6) + " MB written");
            ChatTweaks.logger.debug("cache file created successfully");

            //TODO: calculate SHA?

            return true;
        } catch (UnknownHostException e) {
            ChatTweaks.logger.error("Can't connect to external Server, network connection may be interrupted: ", e);
            //TODO: check if network connection is even possible aka "minecraft online mode"
        } catch (IOException e) {
            ChatTweaks.logger.error("An error occurred trying to load from an API: ", e);
        }

        return false;
    }

    public static boolean invalidateCacheFile(File cacheFile) {
        ChatTweaks.logger.debug("invalidating cache file...");
        return cacheFile.setLastModified(cacheFile.lastModified() - DEFAULT_CACHE_TIME * 2);
    }

    public static File getCacheDirectory() {
        File file = new File(Minecraft.getMinecraft().mcDataDir, "ChatTweaks/cache/");
        if (!file.exists() && !file.mkdirs()) {
            throw new RuntimeException("Could not create cache directory for Chat Tweaks.");
        }
        return file;
    }

}
