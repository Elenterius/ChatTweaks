package net.blay09.mods.chattweaks.chat.emotes.twitch;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;
import net.blay09.mods.chattweaks.ChatTweaks;
import net.blay09.mods.chattweaks.balyware.CachedAPI;
import net.blay09.mods.chattweaks.chat.emotes.IEmote;
import net.minecraft.util.IntHashMap;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class TwitchEmotesAPI {

    public static final String CLIENT_ID = "gdhi94otnk7c7746syjv7gkr6bizq4w";

    public static final int EMOTESET_GLOBAL = 0;
    public static final int EMOTESET_TURBO = 19194;

    private static final IntHashMap<String> emoteSets = new IntHashMap<>();
    private static final IntHashMap<IEmote> twitchEmotes = new IntHashMap<>();

    private static byte restoreAttempts = 0;

    public static boolean loadEmoteSets() throws Exception {
        try {
            Optional<JsonReader> optionalReader = CachedAPI.loadCachedAPI("https://twitchemotes.com/api_cache/v3/sets.json", "twitch_emotesets_v3.json", null);
            if (!optionalReader.isPresent()) {
                throw new IOException();
            }

            JsonReader reader = optionalReader.get(); //TODO: correctly implement Optional use

            int key;
            reader.beginObject();
            while (reader.hasNext()) {
                key = Integer.parseInt(reader.nextName());
                reader.beginObject();
                if (reader.nextName().equals("channel_name")) {
                    emoteSets.addKey(key, reader.nextString());

                    reader.skipValue(); // next token key
                    reader.skipValue(); // value of next token
                    reader.endObject();
                } else {
                    reader.skipValue(); // value of previous token
                    if (reader.nextName().equals("channel_name")) {
                        emoteSets.addKey(key, reader.nextString());
                    } else {
                        reader.skipValue(); // value of current token
                    }
                    reader.endObject();
                }
            }
            reader.endObject();
            reader.close();
            return true;
        } catch (JsonSyntaxException e) {
            ChatTweaks.logger.error("Invalid Json Syntax of File: ", e);
        } catch (JsonIOException e) {
            ChatTweaks.logger.error("An error occurred trying to load a cached API result: ", e);
        } catch (MalformedJsonException e) {
            ChatTweaks.logger.warn("cache file twitch_emotesets_v3.json is corrupted");
            boolean invalidated = CachedAPI.invalidateCacheFile(new File(CachedAPI.getCacheDirectory(), "twitch_emotesets_v3.json"));
            if (invalidated) {
                if (restoreAttempts < 3) {
                    restoreAttempts += 1;
                    if (loadEmoteSets()) {
                        restoreAttempts = 0;
                    } else {
                        ChatTweaks.logger.warn("Restore Attempt " + restoreAttempts + " failed!");
                    }
                }
            }
        }
        return false;
    }

    @Nullable
    public static String getChannelForEmoteSet(int emoteSet) {
        return emoteSets.lookup(emoteSet);
    }

    public static Optional<JsonReader> loadEmotes(int... emoteSets) {
        StringBuilder sb = new StringBuilder();
        for (int emoteSet : emoteSets) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(emoteSet);
        }
        String url = "https://api.twitch.tv/kraken/chat/emoticon_images?client_id=" + CLIENT_ID;
        if (emoteSets.length > 0) {
            url += "&emotesets=" + sb.toString();
        }
        return CachedAPI.loadCachedAPI(url, "twitch_emotes" + (sb.length() > 0 ? "-" + sb.toString() : "") + ".json", "application/vnd.twitchtv.v5+json");
    }

    public static void registerTwitchEmote(int id, IEmote emote) {
        twitchEmotes.addKey(id, emote);
    }

    @Nullable
    public static IEmote getEmoteById(int id) {
        return twitchEmotes.lookup(id);
    }

}
