package com.example.surfacemap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Create / rename / move / recolour / delete a waypoint. Pure data editing, no teleport button. */
public class WaypointEditScreen extends Screen {
    private static final int[] PALETTE = {
            0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55, 0xFF55FFFF, 0xFF5555FF, 0xFFFF55FF, 0xFFFFFFFF };

    private final Screen parent;
    private final Waypoint wp;
    private final boolean isNew;
    private EditBox name, xBox, yBox, zBox;
    private int color;
    private Button colorButton;

    public WaypointEditScreen(Screen parent, Waypoint wp, boolean isNew) {
        super(Component.literal(isNew ? "New Waypoint" : "Edit Waypoint"));
        this.parent = parent; this.wp = wp; this.isNew = isNew; this.color = wp.color;
    }

    @Override
    protected void init() {
        int cx = width / 2, y = height / 2 - 70;
        name = new EditBox(font, cx - 100, y, 200, 20, Component.literal("Name"));
        name.setMaxLength(32); name.setValue(wp.name); addRenderableWidget(name);

        xBox = box(cx - 100, y + 30, 60, wp.x); yBox = box(cx - 30, y + 30, 60, wp.y); zBox = box(cx + 40, y + 30, 60, wp.z);

        colorButton = Button.builder(Component.literal("Colour: ###"), b -> {
            int i = 0;
            for (int k = 0; k < PALETTE.length; k++) if (PALETTE[k] == color) i = k;
            color = PALETTE[(i + 1) % PALETTE.length];
        }).bounds(cx - 100, y + 60, 200, 20).build();
        addRenderableWidget(colorButton);

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save()).bounds(cx - 100, y + 90, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(cx + 2, y + 90, 98, 20).build());
        if (!isNew) {
            addRenderableWidget(Button.builder(Component.literal("Delete"), b -> {
                WaypointManager.remove(wp);
                onClose();
            }).bounds(cx - 100, y + 114, 200, 20).build());
        }
        setInitialFocus(name);
    }

    private EditBox box(int x, int y, int w, int value) {
        EditBox b = new EditBox(font, x, y, w, 20, Component.empty());
        b.setValue(Integer.toString(value));
        addRenderableWidget(b);
        return b;
    }

    private void save() {
        String n = name.getValue().trim();
        wp.name = n.isEmpty() ? "Waypoint" : n;
        wp.x = parse(xBox, wp.x); wp.y = parse(yBox, wp.y); wp.z = parse(zBox, wp.z);
        wp.color = color;
        WaypointManager.upsert(wp);
        onClose();
    }

    private static int parse(EditBox b, int fallback) {
        try { return Integer.parseInt(b.getValue().trim()); } catch (NumberFormatException e) { return fallback; }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, height / 2 - 90, 0xFFFFFFFF);
        g.drawString(font, "X", width / 2 - 100, height / 2 - 50, 0xFFAAAAAA, false);
        g.drawString(font, "Y", width / 2 - 30, height / 2 - 50, 0xFFAAAAAA, false);
        g.drawString(font, "Z", width / 2 + 40, height / 2 - 50, 0xFFAAAAAA, false);
        colorButton.setMessage(Component.literal("Colour  \u25A0").withColor(color & 0xFFFFFF));
    }

    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
}
