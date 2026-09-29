package com.example.paleinfection;

import org.bukkit.plugin.java.JavaPlugin;

public final class PaleInfectionPlugin extends JavaPlugin {
    private InfectionManager infectionManager;
    private MadnessManager madnessManager;
    private ScrunerManager scrunerManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        infectionManager = new InfectionManager(this);
        infectionManager.load();

        madnessManager = new MadnessManager(this, infectionManager);
        scrunerManager = new ScrunerManager(this, infectionManager);

        getServer().getPluginManager().registerEvents(infectionManager, this);
        getServer().getPluginManager().registerEvents(madnessManager, this);
        getServer().getPluginManager().registerEvents(scrunerManager, this);

        PaleCommand command = new PaleCommand(this, infectionManager);
        if (getCommand("pale") != null) {
            getCommand("pale").setExecutor(command);
            getCommand("pale").setTabCompleter(command);
        }

        infectionManager.startTasks();
        madnessManager.start();
        scrunerManager.start();

        getLogger().info("PaleInfection enabled.");
    }

    @Override
    public void onDisable() {
        if (infectionManager != null) infectionManager.save();
    }

    public InfectionManager infection() { return infectionManager; }
}
