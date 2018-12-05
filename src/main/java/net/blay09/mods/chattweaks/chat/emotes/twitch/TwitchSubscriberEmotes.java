package net.blay09.mods.chattweaks.chat.emotes.twitch;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import net.blay09.mods.chattweaks.ChatTweaks;
import net.blay09.mods.chattweaks.ChatTweaksAPI;
import net.blay09.mods.chattweaks.chat.emotes.IEmote;
import net.blay09.mods.chattweaks.chat.emotes.IEmoteGroup;
import net.blay09.mods.chattweaks.chat.emotes.IEmoteLoader;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TwitchSubscriberEmotes implements IEmoteLoader {

    private static final Pattern DEFAULT_VALIDATION_PATTERN = Pattern.compile("[a-z0-9][a-z0-9]+[A-Z0-9].*");
    private static final String URL_TEMPLATE = "https://static-cdn.jtvnw.net/emoticons/v1/{{id}}/1.0";

    public TwitchSubscriberEmotes(String validationRegex) {
        Optional<JsonReader> optionalReader = TwitchEmotesAPI.loadEmotes();

        if (optionalReader.isPresent()) {
            Pattern validationPattern = compilePattern(validationRegex, DEFAULT_VALIDATION_PATTERN);
            Map<Integer, IEmoteGroup> groupMap = Maps.newHashMap();
            Matcher matcher = validationPattern.matcher("");

//        {"emoticons":[
//            {"code":"\\:-?\\)","emoticon_set":0,"id":1},
//            {"code":"\\:-?\\(","emoticon_set":0,"id":2},

            try {
                Gson gson = new Gson();
                JsonReader reader = optionalReader.get();

                reader.beginObject();
                reader.skipValue(); // emoticons token
                reader.beginArray();

                while (reader.hasNext()) {
                    JsonObject entry = gson.fromJson(reader, JsonObject.class);
                    int emoteSet = (!entry.has("emoticon_set") || entry.get("emoticon_set").isJsonNull()) ? TwitchEmotesAPI.EMOTESET_GLOBAL : entry.get("emoticon_set").getAsInt();
                    if (emoteSet == TwitchEmotesAPI.EMOTESET_GLOBAL || emoteSet == TwitchEmotesAPI.EMOTESET_TURBO) {
                        continue;
                    }
                    String code = entry.get("code").getAsString();
                    matcher.reset(code);
                    if (matcher.matches()) {
                        int id = entry.get("id").getAsInt();
                        IEmoteGroup group = groupMap.computeIfAbsent(emoteSet, s -> ChatTweaksAPI.registerEmoteGroup("Twitch-" + s));
                        IEmote emote = ChatTweaksAPI.registerEmote(code, this);
                        emote.setCustomData(id);
                        String channel = TwitchEmotesAPI.getChannelForEmoteSet(emoteSet);
                        if (channel != null) {
                            emote.addTooltip(TextFormatting.GRAY + I18n.format(ChatTweaks.MOD_ID + ":gui.chat.tooltipEmoteChannel") + " " + channel);
                        }
                        emote.setImageCacheFile("/twitch/twitch-" + id);
                        group.addEmote(emote);
                        TwitchEmotesAPI.registerTwitchEmote(id, emote);
                    }
                }

                reader.endArray();
                reader.endObject();
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void loadEmoteImage(IEmote emote) throws Exception {
        ChatTweaksAPI.loadEmoteImage(emote, new URI(URL_TEMPLATE.replace("{{id}}", String.valueOf(emote.getCustomData()))));
    }

    private Pattern compilePattern(String strPattern, Pattern defaultPattern) {
        try {
            return Pattern.compile(strPattern);
        } catch (PatternSyntaxException e) {
            return defaultPattern;
        }
    }

}
