package com.raiderworld.managers;

import com.raiderworld.RaiderWorldPlugin;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarManager {

    private final RaiderWorldPlugin plugin;
    private final Map<UUID, BossBar> playerBars = new HashMap<>();
    private BossBar globalBar; // shared for all online players during raid

    public BossBarManager(RaiderWorldPlugin plugin) {
        this.plugin = plugin;
    }

    public void showPreRaid(long ticksLeft) {
        String timeStr = formatTime(ticksLeft);
        String title = "§c⚔ РЕЙД §7| §fДо начала: §e" + timeStr;
        ensureGlobalBar(title, BarColor.YELLOW, 1.0);
    }

    public void showDuringRaid(int currentWave, int totalWaves, int remainingMobs, double progress) {
        String title = "§c⚔ РЕЙД — ВОЛНА " + currentWave + "/" + totalWaves + " §7| §fОсталось мобов: §e" + remainingMobs;
        ensureGlobalBar(title, BarColor.RED, Math.max(0.0, Math.min(1.0, progress)));
    }

    public void showFinished() {
        String title = "§a✓ РЕЙД ОКОНЧЕН §7| §fВсе волны пройдены!";
        ensureGlobalBar(title, BarColor.GREEN, 1.0);
        // Auto hide after 10 seconds
        Bukkit.getScheduler().runTaskLater(plugin, this::removeAll, 200L);
    }

    private void ensureGlobalBar(String title, BarColor color, double progress) {
        if (globalBar == null) {
            globalBar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
            globalBar.setVisible(true);
            for (Player p : Bukkit.getOnlinePlayers()) {
                globalBar.addPlayer(p);
            }
        } else {
            globalBar.setTitle(title);
            globalBar.setColor(color);
            globalBar.setProgress(progress);
        }
    }

    public void addPlayer(Player player) {
        if (globalBar != null && globalBar.isVisible()) {
            globalBar.addPlayer(player);
        }
    }

    public void removePlayer(Player player) {
        if (globalBar != null) {
            globalBar.removePlayer(player);
        }
    }

    public void removeAll() {
        if (globalBar != null) {
            globalBar.removeAll();
            globalBar.setVisible(false);
            globalBar = null;
        }
        playerBars.clear();
    }

    private String formatTime(long ticks) {
        long totalSeconds = ticks / 20;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (days > 0) {
            return days + "д " + hours + "ч";
        } else if (hours > 0) {
            return hours + "ч " + minutes + "м";
        } else {
            return minutes + "м";
        }
    }
}
