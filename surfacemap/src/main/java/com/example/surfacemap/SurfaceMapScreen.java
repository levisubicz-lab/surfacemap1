package com.example.surfacemap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Fullscreen terrain map. Deliberately NOT drawn here: entities, players, mobs, items, caves,
 * and any "teleport" action. There is no code path that sends a teleport or command.
 */
public class SurfaceMapScreen extends Screen {
    private double camX, camZ;
    private double zoom = 1.0; // pixels per block

    public SurfaceMapScreen() {
        super(Component.literal("World Map"));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) { camX = mc.player.getX(); camZ = mc.player.getZ(); }
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("+ Waypoint here"), b -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) openCreate((int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ()));
        }).bounds(6, 6, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Center on me"), b -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) { camX = mc.player.getX(); camZ = mc.player.getZ(); }
        }).bounds(112, 6, 90, 20).build());
    }

    @Override
    public void tick() {
        // If the player somehow ends up in a disallowed dimension while open, close immediately.
        if (!DimensionRules.isMapAllowed(Minecraft.getInstance().level)) onClose();
    }

    @Override public boolean isPauseScreen() { return false; }

    /** No blur / dim background: this is a full-screen map. */
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    // ---------- coordinate helpers ----------
    private double toWorldX(double sx) { return camX + (sx - width / 2.0) / zoom; }
    private double toWorldZ(double sy) { return camZ + (sy - height / 2.0) / zoom; }
    private int toScreenX(double wx) { return (int) Math.round(width / 2.0 + (wx - camX) * zoom); }
    private int toScreenY(double wz) { return (int) Math.round(height / 2.0 + (wz - camZ) * zoom); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var dim = mc.level.dimension();

        g.fill(0, 0, width, height, 0xFF0B0B10);
        MapStore.flush(dim);

        // --- terrain regions ---
        int rMinX = Math.floorDiv((int) Math.floor(toWorldX(0)), MapStore.REGION_BLOCKS);
        int rMaxX = Math.floorDiv((int) Math.ceil(toWorldX(width)), MapStore.REGION_BLOCKS);
        int rMinZ = Math.floorDiv((int) Math.floor(toWorldZ(0)), MapStore.REGION_BLOCKS);
        int rMaxZ = Math.floorDiv((int) Math.ceil(toWorldZ(height)), MapStore.REGION_BLOCKS);
        for (int rx = rMinX; rx <= rMaxX; rx++) {
            for (int rz = rMinZ; rz <= rMaxZ; rz++) {
                MapStore.Region r = MapStore.get(dim, rx, rz);
                if (r == null) continue;
                int x0 = toScreenX(rx * MapStore.REGION_BLOCKS);
                int y0 = toScreenY(rz * MapStore.REGION_BLOCKS);
                int x1 = toScreenX((rx + 1) * MapStore.REGION_BLOCKS);
                int y1 = toScreenY((rz + 1) * MapStore.REGION_BLOCKS);
                g.blit(RenderPipelines.GUI_TEXTURED, r.id, x0, y0, 0f, 0f, x1 - x0, y1 - y0,
                        MapStore.REGION_BLOCKS, MapStore.REGION_BLOCKS, MapStore.REGION_BLOCKS, MapStore.REGION_BLOCKS);
            }
        }

        // --- waypoints (the ONLY markers on the map; no player arrow, no entities) ---
        Waypoint hovered = null;
        for (Waypoint w : WaypointManager.forDimension(dim.identifier().toString())) {
            int sx = toScreenX(w.x + 0.5), sy = toScreenY(w.z + 0.5);
            if (sx < -20 || sy < -20 || sx > width + 20 || sy > height + 20) continue;
            g.fill(sx - 4, sy - 4, sx + 4, sy + 4, 0xFF000000);
            g.fill(sx - 3, sy - 3, sx + 3, sy + 3, w.color | 0xFF000000);
            g.drawString(font, w.name, sx + 7, sy - 4, 0xFFFFFFFF, true);
            if (Math.abs(mx - sx) <= 5 && Math.abs(my - sy) <= 5) hovered = w;
        }

        // --- footer ---
        String coords = String.format("X %d  Z %d    zoom %.2fx", (int) Math.floor(toWorldX(mx)), (int) Math.floor(toWorldZ(my)), zoom);
        g.drawString(font, coords, 6, height - 12, 0xFFDDDDDD, true);
        g.drawString(font, "Drag: pan   Scroll: zoom   Right-click: new waypoint   Click marker: edit   M/Esc: close",
                6, height - 24, 0xFF888888, true);
        if (hovered != null) {
            g.drawString(font, hovered.name + " (" + hovered.x + ", " + hovered.y + ", " + hovered.z + ")",
                    mx + 10, my + 10, 0xFFFFFF55, true);
        }
        super.render(g, mx, my, pt); // buttons
    }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean dbl) {
        if (super.mouseClicked(event, dbl)) return true; // buttons first
        Minecraft mc = Minecraft.getInstance();
        double mx = event.x(), my = event.y();
        int btn = event.button();

        if (btn == 0) { // left click on a marker => edit
            for (Waypoint w : WaypointManager.forDimension(mc.level.dimension().identifier().toString())) {
                if (Math.abs(mx - toScreenX(w.x + 0.5)) <= 5 && Math.abs(my - toScreenY(w.z + 0.5)) <= 5) {
                    mc.setScreen(new WaypointEditScreen(this, w, false));
                    return true;
                }
            }
        } else if (btn == 1) { // right click => new waypoint at cursor
            int wx = (int) Math.floor(toWorldX(mx)), wz = (int) Math.floor(toWorldZ(my));
            int wy = mc.level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz);
            openCreate(wx, wy, wz);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (event.button() == 0) { camX -= dx / zoom; camZ -= dy / zoom; return true; }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        double before = zoom;
        zoom = Math.max(0.125, Math.min(16.0, zoom * (vAmount > 0 ? 1.25 : 0.8)));
        if (before != zoom) { // keep point under cursor fixed
            double wx = camX + (mx - width / 2.0) / before;
            double wz = camZ + (my - height / 2.0) / before;
            camX = wx - (mx - width / 2.0) / zoom;
            camZ = wz - (my - height / 2.0) / zoom;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (SurfaceMapClient.OPEN.matches(event)) { onClose(); return true; }
        return super.keyPressed(event);
    }

    private void openCreate(int x, int y, int z) {
        Minecraft mc = Minecraft.getInstance();
        Waypoint w = new Waypoint("Waypoint", mc.level.dimension().identifier().toString(), x, y, z, 0xFF55FF55);
        mc.setScreen(new WaypointEditScreen(this, w, true));
    }
}
