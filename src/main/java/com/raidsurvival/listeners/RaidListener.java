package com.raidsurvival.listeners;

import com.raidsurvival.RaidSurvivalPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class RaidListener implements Listener {

    private final RaidSurvivalPlugin plugin;

    public RaidListener(RaidSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.getBossBarManager().addPlayer(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getBossBarManager().removePlayer(e.getPlayer());
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent e) {
        if (plugin.getRaidManager().isActive() && plugin.getConfigManager().isPreventSleep()) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cВо время рейда спать нельзя!");
        }
    }
}
