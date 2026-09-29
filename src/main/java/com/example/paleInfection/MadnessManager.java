package com.example.paleinfection;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class MadnessManager implements Listener {
    private final PaleInfectionPlugin plugin;
    private final InfectionManager infection;
    private final Random random = new Random();
    private final Map<UUID, Integer> sanity = new HashMap<>();
    private BukkitTask task;

    public MadnessManager(PaleInfectionPlugin plugin, InfectionManager infection) { this.plugin=plugin; this.infection=infection; }

    public void start() {
        long interval = Math.max(5, plugin.getConfig().getLong("madness.check-interval-ticks", 10));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    private void tick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!infection.isInfected(p.getLocation())) {
                sanity.put(p.getUniqueId(), Math.max(0, sanity.getOrDefault(p.getUniqueId(), 0) - 1));
                p.removeScoreboardTag("pale_scritching");
                continue;
            }
            p.addScoreboardTag("pale_scritching");
            int s = Math.min(100, sanity.getOrDefault(p.getUniqueId(), 0) + 1);
            sanity.put(p.getUniqueId(), s);
            int duration = plugin.getConfig().getInt("madness.effect-duration-ticks", 50);
            int amp = plugin.getConfig().getInt("madness.amplifier", 0);
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, amp, true, false, false));
            if (s > 40) p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, duration, 0, true, false, false));
            if (s > 25) p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 0, true, false, false));
            if (s > 55) p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 0, true, false, false));
            if (s > 70 && random.nextDouble() < .08) p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, true, false, false));
            infectionAtmosphere(p, s);

            if (random.nextDouble() < plugin.getConfig().getDouble("madness.illusion-chance", .18)) illusion(p, s);
        }
    }

    private void infectionAtmosphere(Player p, int sanity) {
        Location l = p.getLocation();
        World w = p.getWorld();
        int amount = sanity > 70 ? 7 : sanity > 35 ? 4 : 2;
        w.spawnParticle(Particle.WHITE_ASH, l.clone().add(0, 2.1, 0), amount, .8, 1.0, .8, .01);
        if (sanity > 45) {
            w.spawnParticle(Particle.PALE_OAK_LEAVES, l.clone().add(0, 1.0, 0), 2, .7, .6, .7);
        }
        if (sanity > 80 && random.nextDouble() < .025) {
            w.playSound(l, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, .22f, .5f);
        }
    }

    private void illusion(Player p, int sanity) {
        Location eye = p.getEyeLocation();
        int type = random.nextInt(5);
        switch (type) {
            case 0 -> spiderEyes(p);
            case 1 -> whisper(p);
            case 2 -> falseFootsteps(p);
            case 3 -> paleFigure(p);
            default -> pulseVision(p, sanity);
        }
    }

    private void spiderEyes(Player p) {
        Location l = p.getEyeLocation().add(p.getLocation().getDirection().multiply(3));
        p.spawnParticle(Particle.DUST, l, 8, .12, .12, .12, new Particle.DustOptions(Color.RED, 2.0f));
        p.playSound(p.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, .35f, .5f);
    }

    private void whisper(Player p) {
        if (!plugin.getConfig().getBoolean("madness.whispers", true)) return;
        String[] words = {"§8Ты слышишь скрип?", "§8Не оборачивайся.", "§8Оно уже выросло.", "§8Сердце смотрит.", "§8Не доверяй деревьям."};
        p.sendActionBar(net.kyori.adventure.text.Component.text(words[random.nextInt(words.length)]));
        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, .25f, .65f);
    }

    private void falseFootsteps(Player p) {
        Location behind = p.getLocation().subtract(p.getLocation().getDirection().multiply(2));
        p.getWorld().spawnParticle(Particle.PALE_OAK_LEAVES, behind.add(0, .15, 0), 12, .3, .1, .3);
        p.playSound(behind, Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, .45f, .35f);
    }

    private void paleFigure(Player p) {
        Location l = p.getLocation().add(p.getLocation().getDirection().multiply(5));
        p.spawnParticle(Particle.WHITE_ASH, l, 20, .35, 1.0, .35);
        p.spawnParticle(Particle.PALE_OAK_LEAVES, l.clone().add(0,1,0), 12, .3, .7, .3);
    }

    private void pulseVision(Player p, int sanity) {
        float volume = Math.min(1f, .2f + sanity / 150f);
        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_AMBIENT, volume, .35f);
        p.spawnParticle(Particle.SONIC_BOOM, p.getEyeLocation().add(p.getLocation().getDirection().multiply(4)), 1);
    }

    @EventHandler public void quit(PlayerQuitEvent e) { sanity.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void world(PlayerChangedWorldEvent e) { sanity.put(e.getPlayer().getUniqueId(), 0); }
}