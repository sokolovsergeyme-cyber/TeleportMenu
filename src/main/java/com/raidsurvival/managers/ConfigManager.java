package com.raidsurvival.managers;

import com.raidsurvival.RaidSurvivalPlugin;
import com.raidsurvival.models.MobSettings;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ConfigManager {

    private final RaidSurvivalPlugin plugin;
    private FileConfiguration config;

    private int waves = 6;
    private long intervalTicks;
    private boolean forceNight = true;
    private int radius = 45;
    private boolean allowCancel = false;
    private boolean preventSleep = true;

    private final Map<String, MobSettings> mobSettings = new HashMap<>();
    private int rewardXp = 500;
    private final List<ItemStack> rewardItems = new ArrayList<>();

    public ConfigManager(RaidSurvivalPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        load();
    }

    private void load() {
        waves = Math.max(1, config.getInt("settings.waves", 6));
        int days = Math.max(1, config.getInt("settings.interval-days", 5));
        intervalTicks = days * 24L * 60L * 60L * 20L;
        forceNight = config.getBoolean("settings.force-night", true);
        radius = Math.max(10, config.getInt("settings.radius", 45));
        allowCancel = config.getBoolean("settings.allow-cancel", false);
        preventSleep = config.getBoolean("settings.prevent-sleep", true);

        mobSettings.clear();
        ConfigurationSection mobs = config.getConfigurationSection("mobs");
        if (mobs != null) {
            for (String key : mobs.getKeys(false)) {
                ConfigurationSection s = mobs.getConfigurationSection(key);
                if (s == null) continue;
                MobSettings ms = new MobSettings(key);
                int minWave = Math.max(1, s.getInt("min-wave", 1));
                int minCount = Math.max(0, s.getInt("min-count", 5));
                int maxCount = Math.max(minCount, s.getInt("max-count", 10));
                int armorChance = Math.max(0, Math.min(100, s.getInt("armor-chance", 20)));
                ms.setMinWave(minWave);
                ms.setMinCount(minCount);
                ms.setMaxCount(maxCount);
                ms.setArmorChance(armorChance);
                mobSettings.put(key.toUpperCase(), ms);
            }
        }

        rewardXp = config.getInt("rewards.xp", 500);
        rewardItems.clear();
        List<String> items = config.getStringList("rewards.items");
        for (String line : items) {
            String[] p = line.trim().split("\\s+");
            if (p.length >= 1) {
                try {
                    Material mat = Material.valueOf(p[0].toUpperCase());
                    int amount = p.length > 1 ? Integer.parseInt(p[1]) : 1;
                    rewardItems.add(new ItemStack(mat, amount));
                } catch (Exception ignored) {}
            }
        }
    }

    public void setWaves(int v) {
        v = Math.max(1, v);
        waves = v;
        config.set("settings.waves", v);
        plugin.saveConfig();
    }

    public void setIntervalDays(int days) {
        intervalTicks = days * 24L * 60L * 60L * 20L;
        config.set("settings.interval-days", days);
        plugin.saveConfig();
    }

    public void setForceNight(boolean v) {
        forceNight = v;
        config.set("settings.force-night", v);
        plugin.saveConfig();
    }

    public void setRadius(int v) {
        v = Math.max(10, v);
        radius = v;
        config.set("settings.radius", v);
        plugin.saveConfig();
    }

    public void setAllowCancel(boolean v) {
        allowCancel = v;
        config.set("settings.allow-cancel", v);
        plugin.saveConfig();
    }

    public void setPreventSleep(boolean v) {
        preventSleep = v;
        config.set("settings.prevent-sleep", v);
        plugin.saveConfig();
    }

    public void addOrUpdateMob(String type, int minWave, int min, int max) {
        String key = type.toUpperCase();
        MobSettings ms = mobSettings.getOrDefault(key, new MobSettings(key));
        ms.setMinWave(minWave);
        ms.setMinCount(min);
        ms.setMaxCount(max);
        mobSettings.put(key, ms);
        String path = "mobs." + key;
        config.set(path + ".min-wave", minWave);
        config.set(path + ".min-count", min);
        config.set(path + ".max-count", max);
        plugin.saveConfig();
    }

    public void setMobArmor(String type, int chance) {
        MobSettings ms = mobSettings.get(type.toUpperCase());
        if (ms != null) {
            ms.setArmorChance(chance);
            config.set("mobs." + type.toUpperCase() + ".armor-chance", chance);
            plugin.saveConfig();
        }
    }


    public void removeMob(String type) {
        mobSettings.remove(type.toUpperCase());
        config.set("mobs." + type.toUpperCase(), null);
        plugin.saveConfig();
    }

    public void setRewardXp(int xp) {
        rewardXp = xp;
        config.set("rewards.xp", xp);
        plugin.saveConfig();
    }

    // Getters
    public int getWaves() { return waves; }
    public long getIntervalTicks() { return intervalTicks; }
    public boolean isForceNight() { return forceNight; }
    public int getRadius() { return radius; }
    public boolean isAllowCancel() { return allowCancel; }
    public boolean isPreventSleep() { return preventSleep; }
    public Map<String, MobSettings> getMobSettings() { return Collections.unmodifiableMap(mobSettings); }
    public int getRewardXp() { return rewardXp; }
    public List<ItemStack> getRewardItems() { return Collections.unmodifiableList(rewardItems); }
}
