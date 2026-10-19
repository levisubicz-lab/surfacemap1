package com.example.surfacemap;

import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;

/** Single source of truth for where the map may run. Allow-list, so modded dimensions are OFF too. */
public final class DimensionRules {
    private DimensionRules() {}

    public static boolean isMapAllowed(Level level) {
        return level != null && isMapAllowed(level.dimension());
    }

    public static boolean isMapAllowed(ResourceKey<Level> key) {
        return key == Level.OVERWORLD || key == Level.END;   // Nether (and everything else) = disabled
    }
}
