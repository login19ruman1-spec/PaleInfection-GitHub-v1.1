package com.example.paleinfection;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class InfectionManager implements Listener {
    private final PaleInfectionPlugin plugin;
    private final List<Heart> hearts = new ArrayList<>();
    private final Set<String> protectedPlayerBlocks = new HashSet<>();
    private final Random random = new Random();
    private BukkitTask growthTask;
    private BukkitTask visualTask;

    public InfectionManager(PaleInfectionPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "hearts.yml");
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("hearts");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;
            String world = s.getString("world");
            if (world == null) continue;
            Heart heart = new Heart(world, s.getInt("x"), s.getInt("y"), s.getInt("z"),
                    s.getLong("created", System.currentTimeMillis()), s.getInt("radius", 1), s.getBoolean("treeGrown"));
            ConfigurationSection biomes = s.getConfigurationSection("original-biomes");
            if (biomes != null) {
                for (String biomeKey : biomes.getKeys(false)) {
                    String value = biomes.getString(biomeKey);
                    if (value != null) heart.originalBiomes().put(biomeKey, value);
                }
            }
            hearts.add(heart);
        }
    }

    public void save() {
        File file = new File(plugin.getDataFolder(), "hearts.yml");
        YamlConfiguration cfg = new YamlConfiguration();
        int i = 0;
        for (Heart h : hearts) {
            String p = "hearts." + (i++);
            cfg.set(p + ".world", h.world()); cfg.set(p + ".x", h.x()); cfg.set(p + ".y", h.y()); cfg.set(p + ".z", h.z());
            cfg.set(p + ".created", h.createdAt()); cfg.set(p + ".radius", h.radius()); cfg.set(p + ".treeGrown", h.treeGrown());
            for (Map.Entry<String, String> biome : h.originalBiomes().entrySet()) {
                cfg.set(p + ".original-biomes." + biome.getKey(), biome.getValue());
            }
        }
        try { cfg.save(file); } catch (IOException e) { plugin.getLogger().warning("Could not save hearts.yml: " + e.getMessage()); }
    }

    public void startTasks() {
        long interval = Math.max(20, plugin.getConfig().getLong("infection.growth-interval-ticks", 100));
        growthTask = Bukkit.getScheduler().runTaskTimer(plugin, this::growAll, interval, interval);
        long particleInterval = Math.max(1, plugin.getConfig().getLong("heart.particle-interval-ticks", 10));
        visualTask = Bukkit.getScheduler().runTaskTimer(plugin, this::pulseAll, particleInterval, particleInterval);
    }

    private void growAll() {
        for (Heart heart : new ArrayList<>(hearts)) {
            Location loc = heart.location(plugin.getServer());
            if (loc == null) continue;
            if (heart.radius() < plugin.getConfig().getInt("infection.max-radius", 42)) {
                int amount = Math.max(1, plugin.getConfig().getInt("infection.blocks-per-growth", 4));
                spreadPaleMoss(heart, amount);
                int newRadius = Math.min(plugin.getConfig().getInt("infection.max-radius", 42), heart.radius() + 1);
                convertBiomeRing(heart, newRadius);
                heart.radius(newRadius);
                if (!heart.treeGrown() && System.currentTimeMillis() - heart.createdAt() >= plugin.getConfig().getLong("heart.tree-growth-delay-ticks", 2400) * 50L) {
                    growHeartTree(heart);
                    heart.treeGrown(true);
                }
            }
        }
        save();
    }

    private void spreadPaleMoss(Heart heart, int amount) {
        Location center = heart.location(plugin.getServer());
        if (center == null) return;
        World w = center.getWorld();
        int radius = Math.max(2, heart.radius());
        int placed = 0;
        for (int tries = 0; tries < amount * 8 && placed < amount; tries++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = Math.max(2, radius + random.nextInt(3) - 1);
            int x = center.getBlockX() + (int)Math.round(Math.cos(angle) * dist);
            int z = center.getBlockZ() + (int)Math.round(Math.sin(angle) * dist);
            int y = findGroundY(w, x, center.getBlockY(), z);
            if (y == Integer.MIN_VALUE) continue;
            Block ground = w.getBlockAt(x, y, z);
            Block above = ground.getRelative(BlockFace.UP);
            if (!above.getType().isAir()) continue;
            if (!isReplaceableGround(ground.getType())) continue;
            if (isRedstoneProtected(above.getLocation(), center)) continue;
            above.setType(Material.PALE_MOSS_BLOCK, false);
            placed++;
            if (random.nextDouble() < .30) above.getRelative(BlockFace.UP).setType(Material.PALE_HANGING_MOSS, false);
        }
    }

    private int findGroundY(World w, int x, int aroundY, int z) {
        int top = Math.min(w.getMaxHeight() - 2, aroundY + 18);
        int bottom = Math.max(w.getMinHeight() + 1, aroundY - 18);
        for (int y = top; y >= bottom; y--) {
            Material type = w.getBlockAt(x, y, z).getType();
            if (type.isSolid() && type != Material.WATER && type != Material.LAVA) return y;
        }
        return Integer.MIN_VALUE;
    }

    private boolean isReplaceableGround(Material m) {
        if (m == Material.PALE_MOSS_BLOCK || m == Material.PALE_HANGING_MOSS) return false;
        return switch (m) {
            case GRASS_BLOCK, DIRT, COARSE_DIRT, ROOTED_DIRT, PODZOL, MYCELIUM, MOSS_BLOCK, STONE, COBBLESTONE, ANDESITE, DIORITE, GRANITE, SAND, RED_SAND, GRAVEL -> true;
            default -> !plugin.getConfig().getBoolean("infection.only-natural-ish-blocks", true) && m.isSolid();
        };
    }

    private boolean isRedstoneProtected(Location loc, Location heart) {
        int safe = plugin.getConfig().getInt("infection.redstone-safe-radius", 5);
        for (int dx = -safe; dx <= safe; dx++) for (int dz = -safe; dz <= safe; dz++) for (int dy = -2; dy <= 2; dy++) {
            if (dx*dx + dz*dz > safe*safe) continue;
            Block b = loc.getWorld().getBlockAt(loc.getBlockX()+dx, loc.getBlockY()+dy, loc.getBlockZ()+dz);
            if (b.getType() == Material.REDSTONE_BLOCK) return true;
        }
        return false;
    }

    private void pulseAll() {
        for (Heart heart : new ArrayList<>(hearts)) {
            Location l = heart.location(plugin.getServer());
            if (l == null) continue;
            World w = l.getWorld();
            w.spawnParticle(Particle.DUST, l.clone().add(.5, .8, .5), 5, .25, .2, .25,
                    new Particle.DustOptions(Color.fromRGB(255, 92, 30), 1.3f));
            w.spawnParticle(Particle.END_ROD, l.clone().add(.5, .1, .5), 2, .12, .05, .12, .01);
            w.playSound(l, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, .12f, .55f);
            // The infection breathes: orange spores rise from the ground around the heart.
            int visualRadius = Math.max(3, Math.min(heart.radius(), 18));
            for (int i = 0; i < 4; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double d = 2 + random.nextDouble() * visualRadius;
                Location p = l.clone().add(Math.cos(a) * d, .15 + random.nextDouble() * .8, Math.sin(a) * d);
                w.spawnParticle(Particle.DUST, p, 2, .15, .15, .15,
                        new Particle.DustOptions(Color.fromRGB(255, 116, 36), .8f));
            }
            w.spawnParticle(Particle.WHITE_ASH, l.clone().add(0, 1.5, 0), 8, Math.min(3, visualRadius / 3.0), 1.2, Math.min(3, visualRadius / 3.0), .01);
        }
    }

    private void growHeartTree(Heart heart) {
        Location c = heart.location(plugin.getServer());
        if (c == null) return;
        World w = c.getWorld();
        int r = plugin.getConfig().getInt("heart.tree-radius", 3);
        for (int dx=-r; dx<=r; dx++) for (int dz=-r; dz<=r; dz++) {
            if (dx*dx+dz*dz > r*r) continue;
            int height = 2 + random.nextInt(3);
            for (int y=0; y<height; y++) {
                Block b = w.getBlockAt(c.getBlockX()+dx, c.getBlockY()+y, c.getBlockZ()+dz);
                if (b.getType().isAir() || b.getType() == Material.PALE_MOSS_BLOCK) b.setType(Material.PALE_OAK_LOG, false);
            }
        }
        for (int dx=-r-1; dx<=r+1; dx++) for (int dz=-r-1; dz<=r+1; dz++) {
            if (dx*dx+dz*dz > (r+1)*(r+1)) continue;
            Block b = w.getBlockAt(c.getBlockX()+dx, c.getBlockY()+3, c.getBlockZ()+dz);
            if (b.getType().isAir()) b.setType(Material.PALE_OAK_LEAVES, false);
        }
        w.spawnParticle(Particle.PALE_OAK_LEAVES, c.clone().add(.5, 2.5, .5), 30, 1.5, 1, 1.5);
        w.playSound(c, Sound.ITEM_BONE_MEAL_USE, .8f, .55f);
    }

    public Heart createHeart(Location location) {
        Heart h = new Heart(location.getBlock().getLocation());
        hearts.add(h);
        Block b = location.getBlock();
        b.setType(Material.CRYING_OBSIDIAN, false);
        Block light = b.getRelative(BlockFace.DOWN);
        if (light.getType().isAir()) light.setType(Material.OCHRE_FROGLIGHT, false);
        int startRadius = Math.max(2, plugin.getConfig().getInt("infection.start-radius", 6));
        convertBiomeRing(h, startRadius);
        h.radius(startRadius);
        save();
        return h;
    }

    public boolean removeNearest(Location from, double maxDistance) {
        Heart nearest = null; double best = maxDistance * maxDistance;
        for (Heart h : hearts) {
            Location l = h.location(plugin.getServer());
            if (l == null || !l.getWorld().equals(from.getWorld())) continue;
            double d = l.distanceSquared(from);
            if (d < best) { best = d; nearest = h; }
        }
        if (nearest == null) return false;
        removeHeart(nearest);
        return true;
    }

    /** Converts the newly reached surface area into Pale Garden and remembers the original biome.
     *  Block biomes are 3D in modern Paper, but a surface sample at the heart Y is enough to
     *  make the visible overworld biome change while keeping storage small and persistent.
     */
    private void convertBiomeRing(Heart heart, int radius) {
        if (!plugin.getConfig().getBoolean("biome.enabled", true)) return;
        Location c = heart.location(plugin.getServer());
        if (c == null) return;
        World w = c.getWorld();
        int cx = c.getBlockX(), cz = c.getBlockZ(), y = c.getBlockY();
        int r = Math.max(1, radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                if (isRedstoneProtected(w.getBlockAt(cx + dx, y, cz + dz).getLocation(), c)) continue;
                String key = dx + ":" + dz;
                if (!heart.originalBiomes().containsKey(key)) {
                    Biome old = w.getBlockAt(cx + dx, y, cz + dz).getBiome();
                    heart.originalBiomes().put(key, old.getKey().toString());
                }
                w.getBlockAt(cx + dx, y, cz + dz).setBiome(Biome.PALE_GARDEN);
            }
        }
    }

    private void restoreBiomes(Heart heart) {
        if (!plugin.getConfig().getBoolean("biome.enabled", true)) return;
        Location c = heart.location(plugin.getServer());
        if (c == null) return;
        World w = c.getWorld();
        int cx = c.getBlockX(), cz = c.getBlockZ(), y = c.getBlockY();
        for (Map.Entry<String, String> entry : heart.originalBiomes().entrySet()) {
            String[] parts = entry.getKey().split(":");
            if (parts.length != 2) continue;
            try {
                int dx = Integer.parseInt(parts[0]);
                int dz = Integer.parseInt(parts[1]);
                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(entry.getValue());
                Biome old = key == null ? null : org.bukkit.Registry.BIOME.get(key);
                if (old != null) w.getBlockAt(cx + dx, y, cz + dz).setBiome(old);
            } catch (IllegalArgumentException ignored) {
                // Ignore a biome removed by a datapack; the rest of the area is still restored.
            }
        }
        heart.originalBiomes().clear();
    }

    public void removeHeart(Heart h) {
        Location c = h.location(plugin.getServer());
        if (c != null) {
            Block b = c.getBlock();
            if (b.getType() == Material.CRYING_OBSIDIAN) {
                b.setType(Material.AIR, false);
                Block light = b.getRelative(BlockFace.DOWN);
                if (light.getType() == Material.OCHRE_FROGLIGHT) light.setType(Material.AIR, false);
            }
            cleanInfection(h);
            restoreBiomes(h);
        }
        hearts.remove(h);
        save();
    }

    private void cleanInfection(Heart h) {
        Location c = h.location(plugin.getServer());
        if (c == null) return;
        World w = c.getWorld();
        int r = Math.min(plugin.getConfig().getInt("infection.max-radius", 42), h.radius() + 2);
        for (int x=-r; x<=r; x++) for (int z=-r; z<=r; z++) {
            if (x*x+z*z > r*r) continue;
            for (int dy=-2; dy<=4; dy++) {
                Block b = w.getBlockAt(c.getBlockX()+x, c.getBlockY()+dy, c.getBlockZ()+z);
                if (b.getType() == Material.PALE_MOSS_BLOCK || b.getType() == Material.PALE_HANGING_MOSS ||
                        b.getType() == Material.PALE_OAK_LOG || b.getType() == Material.PALE_OAK_LEAVES) b.setType(Material.AIR, false);
            }
        }
    }

    public boolean isInfected(Location loc) {
        for (Heart h : hearts) {
            Location c = h.location(plugin.getServer());
            if (c == null || !c.getWorld().equals(loc.getWorld())) continue;
            double max = Math.max(1, h.radius());
            if (c.distanceSquared(loc) <= max * max) return true;
        }
        return false;
    }

    public Heart nearest(Location loc) {
        Heart result = null; double best = Double.MAX_VALUE;
        for (Heart h : hearts) {
            Location c = h.location(plugin.getServer());
            if (c == null || !c.getWorld().equals(loc.getWorld())) continue;
            double d = c.distanceSquared(loc);
            if (d < best) { best = d; result = h; }
        }
        return result;
    }

    public List<Heart> hearts() { return Collections.unmodifiableList(hearts); }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        if (b.getType() == Material.CRYING_OBSIDIAN) {
            Heart found = null;
            for (Heart h : hearts) {
                Location l = h.location(plugin.getServer());
                if (l != null && l.getBlock().equals(b)) { found = h; break; }
            }
            if (found != null) {
                event.setDropItems(false);
                removeHeart(found);
                event.getPlayer().sendMessage("§6Сердце уничтожено. Бледный мох начинает увядать...");
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("infection.protect-players-built-blocks", true)) return;
        if (isInfected(event.getBlock().getLocation())) protectedPlayerBlocks.add(key(event.getBlock().getLocation()));
    }

    private String key(Location l) { return l.getWorld().getName()+":"+l.getBlockX()+":"+l.getBlockY()+":"+l.getBlockZ(); }
}
