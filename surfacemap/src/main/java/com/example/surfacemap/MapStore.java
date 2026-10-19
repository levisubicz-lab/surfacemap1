package com.example.surfacemap;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;

/**
 * Top-down terrain cache. SURFACE ONLY: colour comes from the world-surface heightmap, i.e. the
 * highest non-air block in each column. Nothing below that block is ever read, so caves, tunnels
 * and bases cannot appear, and the player's Y level is irrelevant.
 */
public final class MapStore {
    public static final int REGION_BLOCKS = 512; // 32x32 chunks per region texture

    public static final class Region {
        public final Identifier id;
        public final DynamicTexture texture;
        boolean dirty;

        Region(ResourceKey<Level> dim, int rx, int rz) {
            this.id = Identifier.fromNamespaceAndPath("surfacemap",
                    "region/" + dim.identifier().getPath() + "_" + rx + "_" + rz);
            this.texture = new DynamicTexture(() -> "surfacemap_region", REGION_BLOCKS, REGION_BLOCKS, true);
            Minecraft.getInstance().getTextureManager().register(id, texture);
        }

        void close() {
            Minecraft.getInstance().getTextureManager().release(id);
        }
    }

    private static final Map<ResourceKey<Level>, Map<Long, Region>> REGIONS = new HashMap<>();

    private MapStore() {}

    private static long key(int rx, int rz) { return ((long) rx << 32) | (rz & 0xFFFFFFFFL); }

    public static Region get(ResourceKey<Level> dim, int rx, int rz) {
        Map<Long, Region> m = REGIONS.get(dim);
        return m == null ? null : m.get(key(rx, rz));
    }

    private static Region getOrCreate(ResourceKey<Level> dim, int rx, int rz) {
        return REGIONS.computeIfAbsent(dim, d -> new HashMap<>())
                .computeIfAbsent(key(rx, rz), k -> new Region(dim, rx, rz));
    }

    public static void clear() {
        REGIONS.values().forEach(m -> m.values().forEach(Region::close));
        REGIONS.clear();
    }

    /** Re-uploads any region touched since the last call. Called from the screen's render path. */
    public static void flush(ResourceKey<Level> dim) {
        Map<Long, Region> m = REGIONS.get(dim);
        if (m == null) return;
        for (Region r : m.values()) if (r.dirty) { r.texture.upload(); r.dirty = false; }
    }

    public static void scan(ClientLevel level, LevelChunk chunk) {
        if (!DimensionRules.isMapAllowed(level)) return;        // hard stop: never map the Nether

        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        int rx = Math.floorDiv(baseX, REGION_BLOCKS);
        int rz = Math.floorDiv(baseZ, REGION_BLOCKS);
        Region region = getOrCreate(level.dimension(), rx, rz);
        NativeImage img = region.texture.getPixels();
        if (img == null) return;

        int ox = baseX - rx * REGION_BLOCKS;
        int oz = baseZ - rz * REGION_BLOCKS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int h = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                int hNorth = z > 0 ? chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z - 1) : h;
                pos.set(baseX + x, h - 1, baseZ + z);
                BlockState st = chunk.getBlockState(pos);

                int argb = 0; // transparent = unexplored / void
                if (!st.isAir()) {
                    int rgb = st.getMapColor(level, pos).col;
                    if (rgb != 0) {
                        double shade = h > hNorth ? 1.12 : (h < hNorth ? 0.86 : 1.0);
                        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * shade));
                        int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * shade));
                        int b = Math.min(255, (int) ((rgb & 0xFF) * shade));
                        argb = 0xFF000000 | (r << 16) | (g << 8) | b;
                    }
                }
                img.setPixel(ox + x, oz + z, argb);
            }
        }
        region.dirty = true;
    }
}
