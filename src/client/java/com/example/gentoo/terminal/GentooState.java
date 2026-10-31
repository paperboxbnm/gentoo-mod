package com.example.gentoo.terminal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Everything the terminal remembers, and the code that keeps it on disk between game launches. */
public final class GentooState {

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("gentoo-mod.json");
    private static GentooState instance;

    public int stage;
    public boolean chroot;
    public boolean finished;
    /** When set, the terminal types the install by itself. Turned off as soon as it finishes. */
    public boolean autopilot;
    public String cwd = "/";
    public final List<String> log = new ArrayList<>();
    public final List<String> history = new ArrayList<>();

    private GentooState() {
    }

    public static GentooState get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public boolean done() {
        return stage >= Flow.STAGES.size();
    }

    public String prompt() {
        return (chroot ? "(chroot) livecd " + cwd : "livecd " + cwd) + " # ";
    }

    public void say(String... lines) {
        for (String line : lines) {
            log.add(line);
        }
        trim();
    }

    private void trim() {
        while (log.size() > 600) {
            log.remove(0);
        }
    }

    private static GentooState load() {
        GentooState st = new GentooState();
        try {
            if (!Files.exists(FILE)) {
                return st;
            }
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            st.stage = o.has("stage") ? o.get("stage").getAsInt() : 0;
            st.chroot = o.has("chroot") && o.get("chroot").getAsBoolean();
            st.finished = o.has("finished") && o.get("finished").getAsBoolean();
            st.autopilot = o.has("autopilot") && o.get("autopilot").getAsBoolean();
            st.cwd = o.has("cwd") ? o.get("cwd").getAsString() : "/";
            if (o.has("log")) {
                for (var e : o.getAsJsonArray("log")) {
                    st.log.add(e.getAsString());
                }
            }
            if (o.has("history")) {
                for (var e : o.getAsJsonArray("history")) {
                    st.history.add(e.getAsString());
                }
            }
        } catch (Exception ignored) {
            // corrupted save: start over instead of crashing the game
            return new GentooState();
        }
        return st;
    }

    public void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("stage", stage);
            o.addProperty("chroot", chroot);
            o.addProperty("finished", finished);
            o.addProperty("autopilot", autopilot);
            o.addProperty("cwd", cwd);
            JsonArray logJson = new JsonArray();
            int from = Math.max(0, log.size() - 300);
            for (int i = from; i < log.size(); i++) {
                logJson.add(log.get(i));
            }
            o.add("log", logJson);
            JsonArray histJson = new JsonArray();
            int hFrom = Math.max(0, history.size() - 100);
            for (int i = hFrom; i < history.size(); i++) {
                histJson.add(history.get(i));
            }
            o.add("history", histJson);
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, o.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    public void reset() {
        stage = 0;
        chroot = false;
        finished = false;
        autopilot = false;
        cwd = "/";
        log.clear();
        history.clear();
        save();
    }
}
