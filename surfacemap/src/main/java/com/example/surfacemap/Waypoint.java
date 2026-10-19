package com.example.surfacemap;

import java.util.UUID;

/** Plain data class (Gson-serialised). */
public class Waypoint {
    public String id = UUID.randomUUID().toString();
    public String name = "Waypoint";
    public String dimension = "minecraft:overworld";
    public int x, y, z;
    public int color = 0xFF55FF55; // ARGB

    public Waypoint() {}
    public Waypoint(String name, String dimension, int x, int y, int z, int color) {
        this.name = name; this.dimension = dimension; this.x = x; this.y = y; this.z = z; this.color = color;
    }
}
