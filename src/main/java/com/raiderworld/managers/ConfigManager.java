package com.raiderworld.managers;

import com.raiderworld.RaiderWorldPlugin;
import com.raiderworld.models.MobSettings;
import com.raiderworld.models.WaveConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ConfigManager {

    private final RaiderWorldPlugin plugin;
    private FileConfiguration config;

    // Global settings
    private int waves = 6;
    private long intervalTicks = 5L * 24 * 60 * 60 * 20; // 5 days default
    private boolean forceNight = true;
    private int radius = 50;
    private String difficulty = "HARD";
    private boolean allowCancel = false;

    // Mob settings: type -> settings
    private final Map<String, MobSettings> mobSettings = new HashMap<>();

    // Wave configs
    private final Map<Integer, WaveConfig> waveConfigs = new HashMap<>();

    // Spawn point
    private String spawnWorld = null;
    private double spawnX, spawnY, spawnZ;

    public ConfigManager(RaiderWorldPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        loadDefaultsIfNeeded();
        loadSettings();
    }

    private void loadDefaultsIfNeeded() {
        config.addDefault("settings.waves", 6);
        config.addDefault("settings.interval-days", 5);
        config.addDefault("settings.force-night", true);
        config.addDefault("settings.radius", 50);
        config.addDefault("settings.difficulty", "HARD");
        config.addDefault("settings.allow-cancel", false);

        // Default mobs
        if (!config.isConfigurationSection("mobs")) {
            config.set("mobs.zombie.min-wave", 1);
            config.set("mobs.zombie.min-count", 20);
            config.set("mobs.zombie.max-count", 40);
            config.set("mobs.zombie.armor-chance", 40);
            config.set("mobs.zombie.weapon-chance", 15);
            config.set("mobs.zombie.incomplete-armor-chance", 30);

            config.set("mobs.skeleton.min-wave", 1);
            config.set("mobs.skeleton.min-count", 5);
            config.set("mobs.skeleton.max-count", 15);
            config.set("mobs.skeleton.armor-chance", 25);
            config.set("mobs.skeleton.weapon-chance", 80);

            config.set("mobs.husk.min-wave", 2);
            config.set("mobs.husk.min-count", 5);
            config.set("mobs.husk.max-count", 20);
        }

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    private void loadSettings() {
        waves = config.getInt("settings.waves", 6);
        int days = config.getInt("settings.interval-days", 5);
        intervalTicks = days * 24L * 60 * 60 * 20;
        forceNight = config.getBoolean("settings.force-night", true);
        radius = config.getInt("settings.radius", 50);
        difficulty = config.getString("settings.difficulty", "HARD");
        allowCancel = config.getBoolean("settings.allow-cancel", false);

        mobSettings.clear();
        ConfigurationSection mobsSection = config.getConfigurationSection("mobs");
        if (mobsSection != null) {
            for (String key : mobsSection.getKeys(false)) {
                ConfigurationSection sec = mobsSection.getConfigurationSection(key);
                if (sec == null) continue;
                MobSettings ms = new MobSettings(key.toUpperCase());
                ms.setMinWave(sec.getInt("min-wave", 1));
                ms.setMinCount(sec.getInt("min-count", 5));
                ms.setMaxCount(sec.getInt("max-count", 15));
                ms.setArmorChance(sec.getInt("armor-chance", 20));
                ms.setWeaponChance(sec.getInt("weapon-chance", 10));
                ms.setIncompleteArmorChance(sec.getInt("incomplete-armor-chance", 25));
                // Effects can be extended later
                mobSettings.put(key.toUpperCase(), ms);
            }
        }

        // Load spawn
        if (config.contains("spawn.world")) {
            spawnWorld = config.getString("spawn.world");
            spawnX = config.getDouble("spawn.x");
            spawnY = config.getDouble("spawn.y");
            spawnZ = config.getDouble("spawn.z");
        }
    }

    // Setters that also save
    public void setWaves(int waves) {
        this.waves = waves;
        config.set("settings.waves", waves);
        plugin.saveConfig();
    }

    public void setIntervalDays(int days) {
        this.intervalTicks = days * 24L * 60 * 60 * 20;
        config.set("settings.interval-days", days);
        plugin.saveConfig();
    }

    public void setForceNight(boolean value) {
        this.forceNight = value;
        config.set("settings.force-night", value);
        plugin.saveConfig();
    }

    public void setRadius(int radius) {
        this.radius = radius;
        config.set("settings.radius", radius);
        plugin.saveConfig();
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
        config.set("settings.difficulty", difficulty);
        plugin.saveConfig();
    }

    public void setAllowCancel(boolean value) {
        this.allowCancel = value;
        config.set("settings.allow-cancel", value);
        plugin.saveConfig();
    }

    public void addOrUpdateMob(String type, int minWave, int minCount, int maxCount) {
        String key = type.toUpperCase();
        MobSettings ms = mobSettings.getOrDefault(key, new MobSettings(key));
        ms.setMinWave(minWave);
        ms.setMinCount(minCount);
        ms.setMaxCount(maxCount);
        mobSettings.put(key, ms);

        String path = "mobs." + type.toLowerCase();
        config.set(path + ".min-wave", minWave);
        config.set(path + ".min-count", minCount);
        config.set(path + ".max-count", maxCount);
        plugin.saveConfig();
    }

    public void setMobArmorChance(String type, int chance) {
        MobSettings ms = mobSettings.get(type.toUpperCase());
        if (ms != null) {
            ms.setArmorChance(chance);
            config.set("mobs." + type.toLowerCase() + ".armor-chance", chance);
            plugin.saveConfig();
        }
    }

    public void setMobWeaponChance(String type, int chance) {
        MobSettings ms = mobSettings.get(type.toUpperCase());
        if (ms != null) {
            ms.setWeaponChance(chance);
            config.set("mobs." + type.toLowerCase() + ".weapon-chance", chance);
            plugin.saveConfig();
        }
    }

    public void removeMob(String type) {
        mobSettings.remove(type.toUpperCase());
        config.set("mobs." + type.toLowerCase(), null);
        plugin.saveConfig();
    }

    public void setSpawn(String world, double x, double y, double z) {
        this.spawnWorld = world;
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
        config.set("spawn.world", world);
        config.set("spawn.x", x);
        config.set("spawn.y", y);
        config.set("spawn.z", z);
        plugin.saveConfig();
    }

    // Getters
    public int getWaves() { return waves; }
    public long getIntervalTicks() { return intervalTicks; }
    public boolean isForceNight() { return forceNight; }
    public int getRadius() { return radius; }
    public String getDifficulty() { return difficulty; }
    public boolean isAllowCancel() { return allowCancel; }
    public Map<String, MobSettings> getMobSettings() { return Collections.unmodifiableMap(mobSettings); }
    public String getSpawnWorld() { return spawnWorld; }
    public double getSpawnX() { return spawnX; }
    public double getSpawnY() { return spawnY; }
    public double getSpawnZ() { return spawnZ; }
    public boolean hasFixedSpawn() { return spawnWorld != null; }
}
