package net.blay09.mods.chattweaks.chat.emotes.twitch;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import net.blay09.mods.chattweaks.ChatTweaks;
import net.blay09.mods.chattweaks.ChatTweaksAPI;
import net.blay09.mods.chattweaks.balyware.CachedAPI;
import net.blay09.mods.chattweaks.chat.emotes.IEmote;
import net.blay09.mods.chattweaks.chat.emotes.IEmoteGroup;
import net.blay09.mods.chattweaks.chat.emotes.IEmoteLoader;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

import java.net.URI;
import java.util.Optional;

public class FFZEmotes implements IEmoteLoader {

    public FFZEmotes() {
        Optional<JsonReader> optionalReader = CachedAPI.loadCachedAPI("https://api.frankerfacez.com/v1/set/global", "ffz_emotes.json", null);
        if (optionalReader.isPresent()) {
            Gson gson = new Gson();
            try {
                JsonObject root = gson.fromJson(optionalReader.get(), JsonObject.class);
                if (root == null) {
                    ChatTweaks.logger.error("Failed to load FrankerFaceZ emotes.");
                    return;
                }
                IEmoteGroup group = ChatTweaksAPI.registerEmoteGroup("FFZ");
                JsonArray defaultSets = root.getAsJsonArray("default_sets");
                JsonObject sets = root.getAsJsonObject("sets");
                for (int i = 0; i < defaultSets.size(); i++) {
                    int setId = defaultSets.get(i).getAsInt();
                    JsonObject set = sets.getAsJsonObject(String.valueOf(setId));
                    JsonArray emoticons = set.getAsJsonArray("emoticons");
                    for (int j = 0; j < emoticons.size(); j++) {
                        JsonObject emoticonObject = emoticons.get(j).getAsJsonObject();
                        String code = emoticonObject.get("name").getAsString();
                        IEmote emote = ChatTweaksAPI.registerEmote(code, this);
                        emote.setCustomData(emoticonObject.getAsJsonObject("urls").get("1").getAsString());
                        emote.addTooltip(TextFormatting.GRAY + I18n.format(ChatTweaks.MOD_ID + ":gui.chat.tooltipFFZEmotes"));
                        emote.setImageCacheFile("ffz-" + emoticonObject.get("id").getAsString());
                        group.addEmote(emote);
                    }
                }
            } catch (Exception e) {
                ChatTweaks.logger.error("Failed to load FrankerFaceZ emotes: ", e);
            }
        } else {
            ChatTweaks.logger.error("Failed to load FrankerFaceZ emotes.");
        }
    }

    @Override
    public void loadEmoteImage(IEmote emote) throws Exception {
        ChatTweaksAPI.loadEmoteImage(emote, new URI("https:" + emote.getCustomData()));
    }

    @Override
    public boolean isCommonEmote(String name) {
        return true;
    }
}
