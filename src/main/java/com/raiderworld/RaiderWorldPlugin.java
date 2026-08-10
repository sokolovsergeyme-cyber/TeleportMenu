package com.raiderworld;

import com.raiderworld.commands.RaidCommand;
import com.raiderworld.listeners.RaidListener;
import com.raiderworld.managers.ConfigManager;
import com.raiderworld.managers.RaidManager;
import com.raiderworld.managers.BossBarManager;
import org.bukkit.plugin.java.JavaPlugin;

public class RaiderWorldPlugin extends JavaPlugin {

    private static RaiderWorldPlugin instance;
    private ConfigManager configManager;
    private RaidManager raidManager;
    private BossBarManager bossBarManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.bossBarManager = new BossBarManager(this);
        this.raidManager = new RaidManager(this);

        // Register commands
        getCommand("raid").setExecutor(new RaidCommand(this));
        getCommand("raid").setTabCompleter(new RaidCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new RaidListener(this), this);

        // Start scheduler for automatic raids
        raidManager.startScheduler();

        getLogger().info("RaiderWorld enabled! Ready for raids on Spigot 26.1.2 / Java 25.");
        getLogger().info("Geyser compatible (Java + Bedrock). Configure via /raid settings ...");
    }

    @Override
    public void onDisable() {
        if (raidManager != null) {
            raidManager.shutdown();
        }
        if (bossBarManager != null) {
            bossBarManager.removeAll();
        }
        getLogger().info("RaiderWorld disabled.");
    }

    public static RaiderWorldPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RaidManager getRaidManager() {
        return raidManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public void reloadPlugin() {
        reloadConfig();
        configManager.reload();
        getLogger().info("Configuration reloaded.");
    }
}
