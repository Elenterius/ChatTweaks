package net.blay09.mods.chattweaks.chat.emotes.twitch;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import net.blay09.mods.chattweaks.ChatTweaks;
import net.blay09.mods.chattweaks.ChatTweaksAPI;
import net.blay09.mods.chattweaks.chat.emotes.IEmote;
import net.blay09.mods.chattweaks.chat.emotes.IEmoteGroup;
import net.blay09.mods.chattweaks.chat.emotes.IEmoteLoader;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

import java.net.URI;
import java.util.Optional;

public class TwitchGlobalEmotes implements IEmoteLoader {

    private static final String URL_TEMPLATE = "https://static-cdn.jtvnw.net/emoticons/v1/{{id}}/1.0";

    public TwitchGlobalEmotes(boolean includeTurbo, boolean includeSmileys) {
        Optional<JsonReader> optionalReader = includeTurbo ? TwitchEmotesAPI.loadEmotes(TwitchEmotesAPI.EMOTESET_GLOBAL, TwitchEmotesAPI.EMOTESET_TURBO) : TwitchEmotesAPI.loadEmotes(TwitchEmotesAPI.EMOTESET_GLOBAL);
        if (optionalReader.isPresent()) {
            Gson gson = new Gson();
            JsonReader reader = optionalReader.get();
            try {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                loadEmotes(root.getAsJsonObject("emoticon_sets").getAsJsonArray(String.valueOf(TwitchEmotesAPI.EMOTESET_GLOBAL)), TextFormatting.GRAY + I18n.format(ChatTweaks.MOD_ID + ":gui.chat.tooltipTwitchEmotes"), includeSmileys, ChatTweaksAPI.registerEmoteGroup("TwitchGlobal"));
                loadEmotes(root.getAsJsonObject("emoticon_sets").getAsJsonArray(String.valueOf(TwitchEmotesAPI.EMOTESET_TURBO)), TextFormatting.GRAY + I18n.format(ChatTweaks.MOD_ID + ":gui.chat.tooltipTwitchTurboEmotes"), includeSmileys, ChatTweaksAPI.registerEmoteGroup("TwitchTurbo"));
            } catch (Exception e) {
                ChatTweaks.logger.error("Failed to load Twitch global emotes: ", e);
            }
        } else {
            ChatTweaks.logger.error("Failed to load Twitch global emotes.");
        }
    }

    private void loadEmotes(JsonArray jsonArray, String tooltip, boolean includeSmileys, IEmoteGroup group) {
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject entry = jsonArray.get(i).getAsJsonObject();
            int id = entry.get("id").getAsInt();
            String code = entry.get("code").getAsString();
            IEmote emote;
            if (code.matches(".*\\p{Punct}.*")) {
                if (!includeSmileys) {
                    continue;
                }
                code = code.replace("\\\\", "\\");
                code = code.replace("&lt\\;", "<");
                code = code.replace("&gt\\;", ">");
                emote = ChatTweaksAPI.registerRegexEmote(code, this);
            } else {
                emote = ChatTweaksAPI.registerEmote(code, this);
            }
            emote.setCustomData(id);
            emote.addTooltip(tooltip);
            emote.setImageCacheFile("twitch-" + id);
            group.addEmote(emote);
            TwitchEmotesAPI.registerTwitchEmote(id, emote);
        }
    }

    @Override
    public void loadEmoteImage(IEmote emote) throws Exception {
        ChatTweaksAPI.loadEmoteImage(emote, new URI(URL_TEMPLATE.replace("{{id}}", String.valueOf(emote.getCustomData()))));
    }

    @Override
    public boolean isCommonEmote(String name) {
        return true;
    }
}
