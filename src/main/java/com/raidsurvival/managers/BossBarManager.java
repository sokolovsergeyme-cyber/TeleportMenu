package com.raidsurvival.managers;

import com.raidsurvival.RaidSurvivalPlugin;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class BossBarManager {

    private final RaidSurvivalPlugin plugin;
    private BossBar bar;

    public BossBarManager(RaidSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    public void showWaiting(long ticksLeft) {
        String time = formatTime(ticksLeft);
        update("§c⚔ РЕЙД §7| §fДо начала: §e" + time, BarColor.YELLOW, 1.0);
    }

    public void showRaid(int wave, int totalWaves, int remaining) {
        String title = "§c⚔ РЕЙД — ВОЛНА " + wave + "/" + totalWaves + " §7| §fОсталось мобов: §e" + remaining;
        double progress = Math.max(0.05, Math.min(1.0, (double) remaining / 40.0)); // примерный прогресс
        update(title, BarColor.RED, progress);
    }

    public void showFinished() {
        update("§a✓ РЕЙД ОКОНЧЕН §7| §fВсе волны пройдены!", BarColor.GREEN, 1.0);
        Bukkit.getScheduler().runTaskLater(plugin, this::removeAll, 200L);
    }

    private void update(String title, BarColor color, double progress) {
        if (bar == null) {
            bar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
            bar.setVisible(true);
            for (Player p : Bukkit.getOnlinePlayers()) {
                bar.addPlayer(p);
            }
        } else {
            bar.setTitle(title);
            bar.setColor(color);
            bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        }
    }

    public void addPlayer(Player player) {
        if (bar != null) bar.addPlayer(player);
    }

    public void removePlayer(Player player) {
        if (bar != null) bar.removePlayer(player);
    }

    public void removeAll() {
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
            bar = null;
        }
    }

    private String formatTime(long ticks) {
        long sec = ticks / 20;
        long days = sec / 86400;
        long hours = (sec % 86400) / 3600;
        long min = (sec % 3600) / 60;
        if (days > 0) return days + "д " + hours + "ч";
        if (hours > 0) return hours + "ч " + min + "м";
        return min + "м";
    }
}
