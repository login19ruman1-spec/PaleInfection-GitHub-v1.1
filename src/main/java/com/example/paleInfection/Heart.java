package com.example.paleinfection;

import org.bukkit.Location;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Heart {
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private long createdAt;
    private int radius;
    private boolean treeGrown;
    // Relative X:Z -> original biome key. Saved before this area becomes PALE_GARDEN.
    private final Map<String, String> originalBiomes;

    public Heart(Location loc) {
        this.world = loc.getWorld().getName();
        this.x = loc.getBlockX();
        this.y = loc.getBlockY();
        this.z = loc.getBlockZ();
        this.createdAt = System.currentTimeMillis();
        this.radius = 1;
        this.originalBiomes = new LinkedHashMap<>();
    }

    public Heart(String world, int x, int y, int z, long createdAt, int radius, boolean treeGrown) {
        this.world = world; this.x = x; this.y = y; this.z = z;
        this.createdAt = createdAt; this.radius = radius; this.treeGrown = treeGrown;
        this.originalBiomes = new LinkedHashMap<>();
    }

    public String world() { return world; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public long createdAt() { return createdAt; }
    public int radius() { return radius; }
    public boolean treeGrown() { return treeGrown; }
    public void radius(int radius) { this.radius = radius; }
    public void treeGrown(boolean treeGrown) { this.treeGrown = treeGrown; }
    public Map<String, String> originalBiomes() { return originalBiomes; }

    public Location location(org.bukkit.Server server) {
        var w = server.getWorld(world);
        return w == null ? null : new Location(w, x + .5, y, z + .5);
    }
}
