package eu.donyka.discord.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class RichPresence {
    public String largeImageKey;
    public String largeImageText;
    public String smallImageText;
    public String partyPrivacy;
    public long startTimestamp;
    public String button_label_1;
    public int instance;
    public String partyId;
    public int partySize;
    public long endTimestamp;
    public String details;
    public String joinSecret;
    public String spectateSecret;
    public String smallImageKey;
    public String matchSecret;
    public String button_url_2;
    public String button_label_2;
    public String state;
    public String button_url_1;
    public int partyMax;

    public RichPresence() {}

    public static RichPresenceBuilder builder() {
        return RichPresenceBuilder.builder();
    }

    public JsonObject toJson(long pid, long nonce) {
        JsonObject data = new JsonObject();
        data.addProperty("nonce", nonce);
        data.addProperty("cmd", "SET_ACTIVITY");

        JsonObject args = new JsonObject();
        args.addProperty("pid", pid);

        JsonObject activity = new JsonObject();

        if (isNotNullOrEmpty(state)) {
            activity.addProperty("state", state);
        }

        if (isNotNullOrEmpty(details)) {
            activity.addProperty("details", details);
        }

        if (this.startTimestamp != 0 || this.endTimestamp != 0) {
            JsonObject timestamps = new JsonObject();

            if (this.startTimestamp != 0) {
                timestamps.addProperty("start", this.startTimestamp);
            }

            if (this.endTimestamp != 0) {
                timestamps.addProperty("end", this.endTimestamp);
            }

            activity.add("timestamps", timestamps);
        }

        if (isNotNullOrEmpty(largeImageKey) || isNotNullOrEmpty(largeImageText) || isNotNullOrEmpty(smallImageKey) || isNotNullOrEmpty(smallImageText)) {
            JsonObject assets = new JsonObject();

            if (isNotNullOrEmpty(largeImageKey)) {
                assets.addProperty("large_image", this.largeImageKey);
            }

            if (isNotNullOrEmpty(largeImageText)) {
                assets.addProperty("large_text", this.largeImageText);
            }

            if (isNotNullOrEmpty(smallImageKey)) {
                assets.addProperty("small_image", this.smallImageKey);
            }

            if (isNotNullOrEmpty(smallImageText)) {
                assets.addProperty("small_text", this.smallImageText);
            }

            activity.add("assets", assets);
        }

        if (isNotNullOrEmpty(partyId) || this.partySize > 0 || this.partyMax > 0) {
            JsonObject party = new JsonObject();

            if (isNotNullOrEmpty(partyId)) {
                party.addProperty("id", partyId);
            }

            if (partySize != 0) {
                JsonArray size = new JsonArray();
                size.add(partySize);

                if (partyMax > 0) {
                    size.add(partyMax);
                }

                party.add("size", size);
            }

            if (isNotNullOrEmpty(partyPrivacy)) {
                party.addProperty("privacy", partyPrivacy);
            }

            activity.add("party", party);
        }

        if (isNotNullOrEmpty(matchSecret) || isNotNullOrEmpty(spectateSecret) || isNotNullOrEmpty(joinSecret)) {
            JsonObject secrets = new JsonObject();

            if (isNotNullOrEmpty(matchSecret)) {
                secrets.addProperty("match", matchSecret);
            }

            if (isNotNullOrEmpty(joinSecret)) {
                secrets.addProperty("join", joinSecret);
            }

            if (isNotNullOrEmpty(spectateSecret)) {
                secrets.addProperty("spectate", spectateSecret);
            }

            activity.add("secrets", secrets);
        }

        JsonArray buttons = new JsonArray();
        if (isNotNullOrEmpty(button_label_1) && isNotNullOrEmpty(button_url_1)) {
            JsonObject btn1 = new JsonObject();
            btn1.addProperty("label", button_label_1.substring(0, Math.min(button_label_1.length(), 32)));
            btn1.addProperty("url", button_url_1);
            buttons.add(btn1);
        }
        if (isNotNullOrEmpty(button_label_2) && isNotNullOrEmpty(button_url_2)) {
            JsonObject btn2 = new JsonObject();
            btn2.addProperty("label", button_label_2.substring(0, Math.min(button_label_2.length(), 32)));
            btn2.addProperty("url", button_url_2);
            buttons.add(btn2);
        }
        if (buttons.size() > 0) {
            activity.add("buttons", buttons);
        }

        activity.addProperty("type", 0);
        activity.addProperty("instance", instance != 0);
        args.add("activity", activity);
        data.add("args", args);

        return data;
    }

    private boolean isNotNullOrEmpty(String input) {
        return input != null && !input.trim().isEmpty();
    }
}
