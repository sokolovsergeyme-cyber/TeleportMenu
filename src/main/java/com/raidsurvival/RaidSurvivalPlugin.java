package com.raidsurvival;

import com.raidsurvival.commands.RaidCommand;
import com.raidsurvival.listeners.RaidListener;
import com.raidsurvival.managers.BossBarManager;
import com.raidsurvival.managers.ConfigManager;
import com.raidsurvival.managers.RaidManager;
import org.bukkit.plugin.java.JavaPlugin;

public class RaidSurvivalPlugin extends JavaPlugin {

    private static RaidSurvivalPlugin instance;
    private ConfigManager configManager;
    private RaidManager raidManager;
    private BossBarManager bossBarManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.bossBarManager = new BossBarManager(this);
        this.raidManager = new RaidManager(this);

        RaidCommand raidCommand = new RaidCommand(this);
        if (getCommand("raid") == null) {
            getLogger().severe("Command 'raid' is missing from plugin.yml. Plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("raid").setExecutor(raidCommand);
        getCommand("raid").setTabCompleter(raidCommand);

        getServer().getPluginManager().registerEvents(new RaidListener(this), this);

        raidManager.startScheduler();

        getLogger().info("RaidSurvival enabled (Spigot 26.1.2 / Java 25)");
    }

    @Override
    public void onDisable() {
        if (raidManager != null) raidManager.shutdown();
        if (bossBarManager != null) bossBarManager.removeAll();
        getLogger().info("RaidSurvival disabled.");
    }

    public static RaidSurvivalPlugin getInstance() {
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
        getLogger().info("Config reloaded.");
    }
}
