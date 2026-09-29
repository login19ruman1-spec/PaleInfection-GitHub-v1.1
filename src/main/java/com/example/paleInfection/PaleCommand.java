package com.example.paleinfection;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class PaleCommand implements CommandExecutor, TabCompleter {
    private final PaleInfectionPlugin plugin; private final InfectionManager infection;
    public PaleCommand(PaleInfectionPlugin plugin, InfectionManager infection){this.plugin=plugin;this.infection=infection;}
    @Override public boolean onCommand(CommandSender s, Command c, String label, String[] a){
        if(!s.hasPermission("paleinfection.admin")){s.sendMessage("§cНет прав.");return true;}
        if(a.length==0){help(s);return true;}
        switch(a[0].toLowerCase()){
            case "start" -> { if(!(s instanceof Player p)){s.sendMessage("Только игрок.");return true;} infection.createHeart(p.getLocation()); p.sendMessage("§6Сердце посажено. §fБледный мох просыпается..."); }
            case "stop" -> { if(!(s instanceof Player p)){return true;} boolean ok=infection.removeNearest(p.getLocation(), 64); p.sendMessage(ok?"§aБлижайшее сердце уничтожено.":"§cСердце рядом не найдено."); }
            case "status" -> { if(!(s instanceof Player p)){return true;} Heart h=infection.nearest(p.getLocation()); if(h==null)p.sendMessage("§7Сердец в этом мире не найдено."); else p.sendMessage("§6Сердце: §f"+h.x()+" "+h.y()+" "+h.z()+" §6радиус: §f"+h.radius()); }
            case "reload" -> {plugin.reloadConfig();s.sendMessage("§aКонфигурация перезагружена.");}
            default -> help(s);
        }
        return true;
    }
    private void help(CommandSender s){s.sendMessage("§6/Pale §fstart §7| §fstop §7| §fstatus §7| §freload");}
    @Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args){if(args.length==1){List<String> x=new ArrayList<>();for(String v:new String[]{"start","stop","status","reload"})if(v.startsWith(args[0].toLowerCase()))x.add(v);return x;}return List.of();}
}
