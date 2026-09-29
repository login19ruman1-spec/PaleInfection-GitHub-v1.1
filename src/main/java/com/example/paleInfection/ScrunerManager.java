package com.example.paleinfection;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class ScrunerManager implements Listener {
    private final PaleInfectionPlugin plugin;
    private final InfectionManager infection;
    private final Random random = new Random();
    private final NamespacedKey typeKey;
    private BukkitTask task;

    private enum Type { NORMAL, HUNTER, GUARDIAN, GENIUS }

    public ScrunerManager(PaleInfectionPlugin plugin, InfectionManager infection) {
        this.plugin=plugin; this.infection=infection; this.typeKey=new NamespacedKey(plugin,"scruner_type");
    }

    public void start() {
        long interval = Math.max(100, plugin.getConfig().getLong("scruners.spawn-interval-ticks", 500));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnTick, interval, interval);
    }

    private void spawnTick() {
        if (!plugin.getConfig().getBoolean("scruners.enabled", true)) return;
        int max = plugin.getConfig().getInt("scruners.max-per-heart", 8);
        for (Heart h : infection.hearts()) {
            Location c = h.location(plugin.getServer()); if (c == null) continue;
            long existing = c.getWorld().getNearbyEntities(c, 25, 12, 25).stream().filter(e -> e.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)).count();
            if (existing >= max || random.nextDouble() > plugin.getConfig().getDouble("scruners.spawn-chance", .35)) continue;
            spawn(c);
        }
    }

    private void spawn(Location center) {
        double a=random.nextDouble()*Math.PI*2, d=5+random.nextDouble()*plugin.getConfig().getInt("scruners.spawn-radius",20);
        Location l=center.clone().add(Math.cos(a)*d, 0, Math.sin(a)*d);
        l.setY(center.getWorld().getHighestBlockYAt(l.getBlockX(), l.getBlockZ())+1);
        Type type = chooseType();
        Spider spider=(Spider)center.getWorld().spawnEntity(l, EntityType.SPIDER);
        spider.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.name());
        String name=switch(type){case NORMAL->"§7Скрипун";case HUNTER->"§6Охотник-скрипун";case GUARDIAN->"§cСтраж-скрипун";case GENIUS->"§5Гений-скрипун";};
        spider.customName(net.kyori.adventure.text.Component.text(name)); spider.setCustomNameVisible(true);
        spider.setRemoveWhenFarAway(false);
        double hp=switch(type){case NORMAL->16;case HUNTER->24;case GUARDIAN->38;case GENIUS->60;};
        spider.getAttribute(Attribute.MAX_HEALTH).setBaseValue(hp); spider.setHealth(hp);
        if (type==Type.HUNTER) spider.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(.38);
        if (type==Type.GUARDIAN) spider.getAttribute(Attribute.ARMOR).setBaseValue(8);
        if (type==Type.GENIUS) { spider.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(.42); spider.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(8); }
        center.getWorld().spawnParticle(Particle.PALE_OAK_LEAVES, l, 18, .5,.4,.5);
        center.getWorld().playSound(l, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, .45f, .7f);
    }

    private Type chooseType() {
        double x=random.nextDouble();
        if(x<.55) return Type.NORMAL;
        if(x<.82) return Type.HUNTER;
        if(x<.97) return Type.GUARDIAN;
        return Type.GENIUS;
    }

    @EventHandler public void death(EntityDeathEvent e) {
        if(!(e.getEntity() instanceof Spider s)) return;
        String t=s.getPersistentDataContainer().get(typeKey,PersistentDataType.STRING); if(t==null)return;
        if("GENIUS".equals(t)) e.getDrops().add(new org.bukkit.inventory.ItemStack(Material.ECHO_SHARD));
        if("GUARDIAN".equals(t)) e.getDrops().add(new org.bukkit.inventory.ItemStack(Material.PALE_HANGING_MOSS, 1));
    }
}
