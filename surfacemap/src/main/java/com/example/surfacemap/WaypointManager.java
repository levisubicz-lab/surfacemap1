package com.example.surfacemap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Waypoint storage: one JSON file per server / singleplayer. No teleport logic exists anywhere by design. */
public final class WaypointManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Waypoint> LIST = new ArrayList<>();
    private static Path file;

    private WaypointManager() {}

    public static void load(String serverKey) {
        String safe = serverKey.replaceAll("[^a-zA-Z0-9._-]", "_");
        file = FabricLoader.getInstance().getConfigDir().resolve("surfacemap").resolve(safe + ".json");
        LIST.clear();
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    Waypoint[] arr = GSON.fromJson(r, Waypoint[].class);
                    if (arr != null) LIST.addAll(Arrays.asList(arr));
                }
            }
        } catch (Exception e) {
            System.err.println("[surfacemap] failed to load waypoints: " + e);
        }
    }

    public static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(LIST, w);
            }
        } catch (Exception e) {
            System.err.println("[surfacemap] failed to save waypoints: " + e);
        }
    }

    public static void unload() { save(); LIST.clear(); file = null; }

    public static List<Waypoint> forDimension(String dim) {
        List<Waypoint> out = new ArrayList<>();
        for (Waypoint w : LIST) if (w.dimension.equals(dim)) out.add(w);
        return out;
    }

    /** Add (if new) or keep (if already tracked, since edits mutate the object) and persist. */
    public static void upsert(Waypoint wp) {
        for (Waypoint w : LIST) if (w.id.equals(wp.id)) { save(); return; }
        LIST.add(wp);
        save();
    }

    public static void remove(Waypoint wp) {
        LIST.removeIf(w -> w.id.equals(wp.id));
        save();
    }
}
