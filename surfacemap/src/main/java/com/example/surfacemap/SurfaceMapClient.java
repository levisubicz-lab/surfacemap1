package com.example.surfacemap;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.chunk.LevelChunk;
import org.lwjgl.glfw.GLFW;

public class SurfaceMapClient implements ClientModInitializer {
    public static KeyMapping OPEN;
    private static int tickCounter;

    @Override
    public void onInitializeClient() {
        // ---- Keybinding (default: M). Nothing else opens the map. ----
        OPEN = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.surfacemap.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("surfacemap", "main"))));

        // ---- Keybinding + dimension gate ----
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN.consumeClick()) {
                if (mc.level == null || mc.player == null || mc.screen != null) continue;
                if (!DimensionRules.isMapAllowed(mc.level)) {
                    mc.player.displayClientMessage(Component.translatable("surfacemap.signal_lost"), true);
                } else {
                    mc.setScreen(new SurfaceMapScreen());
                }
            }
            periodicRescan(mc);
        });

        // ---- Terrain capture (surface only, allowed dimensions only) ----
        ClientChunkEvents.CHUNK_LOAD.register((ClientLevel level, LevelChunk chunk) -> MapStore.scan(level, chunk));

        // ---- Per-server waypoint file + cache lifecycle ----
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
            String key = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "singleplayer";
            WaypointManager.load(key);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
            WaypointManager.unload();
            MapStore.clear();
        });
    }

    /** Picks up block changes near the player (building, trees growing, etc.). */
    private static void periodicRescan(Minecraft mc) {
        if (mc.level == null || mc.player == null || !DimensionRules.isMapAllowed(mc.level)) return;
        if (++tickCounter % 40 != 0) return;
        int pcx = mc.player.chunkPosition().x, pcz = mc.player.chunkPosition().z;
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++) {
                LevelChunk c = mc.level.getChunkSource().getChunkNow(pcx + dx, pcz + dz);
                if (c != null) MapStore.scan(mc.level, c);
            }
    }
}
