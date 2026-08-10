package com.raiderworld.managers;

import com.raiderworld.RaiderWorldPlugin;
import com.raiderworld.models.MobSettings;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RaidManager {

    private final RaiderWorldPlugin plugin;
    private final ConfigManager config;
    private final BossBarManager bossBar;

    private boolean raidActive = false;
    private int currentWave = 0;
    private final Set<UUID> activeMobs = new HashSet<>();
    private Location raidCenter;
    private BukkitTask schedulerTask;
    private BukkitTask waveTask;
    private long nextRaidTicks;
    private World raidWorld;

    public RaidManager(RaiderWorldPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.bossBar = plugin.getBossBarManager();
        this.nextRaidTicks = config.getIntervalTicks();
    }

    public void startScheduler() {
        // Update BossBar every second and count down
        schedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (raidActive) return;
                nextRaidTicks -= 20;
                if (nextRaidTicks <= 0) {
                    // Auto start near a random online player or fixed spawn
                    Player target = getRandomOnlinePlayer();
                    if (target != null) {
                        startRaid(target.getLocation());
                    } else if (config.hasFixedSpawn()) {
                        World w = Bukkit.getWorld(config.getSpawnWorld());
                        if (w != null) {
                            startRaid(new Location(w, config.getSpawnX(), config.getSpawnY(), config.getSpawnZ()));
                        }
                    }
                    nextRaidTicks = config.getIntervalTicks();
                } else {
                    bossBar.showPreRaid(nextRaidTicks);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void startRaid(Location center) {
        if (raidActive) {
            return;
        }
        this.raidActive = true;
        this.currentWave = 0;
        this.raidCenter = center.clone();
        this.raidWorld = center.getWorld();
        this.activeMobs.clear();

        if (config.isForceNight()) {
            raidWorld.setTime(18000); // night
        }

        plugin.getLogger().info("Raid started at " + center.getBlockX() + ", " + center.getBlockY() + ", " + center.getBlockZ());
        broadcast("§c§l⚔ РЕЙД НАЧАЛСЯ! §eПодготовьтесь к обороне!");

        startNextWave();
    }

    public void startRaidNearPlayer(Player player) {
        startRaid(player.getLocation());
    }

    private void startNextWave() {
        currentWave++;
        if (currentWave > config.getWaves()) {
            endRaid(true);
            return;
        }

        activeMobs.clear();
        int totalSpawned = 0;

        for (MobSettings ms : config.getMobSettings().values()) {
            if (ms.getMinWave() > currentWave) continue;
            int count = ThreadLocalRandom.current().nextInt(ms.getMinCount(), ms.getMaxCount() + 1);
            for (int i = 0; i < count; i++) {
                Location spawnLoc = getRandomSpawnLocation(raidCenter, config.getRadius());
                LivingEntity mob = spawnConfiguredMob(ms, spawnLoc);
                if (mob != null) {
                    activeMobs.add(mob.getUniqueId());
                    totalSpawned++;
                }
            }
        }

        // Extra stronger mobs on last wave
        if (currentWave == config.getWaves()) {
            spawnMiniBoss(raidCenter);
        }

        double progress = 1.0 - ((double) currentWave / config.getWaves());
        bossBar.showDuringRaid(currentWave, config.getWaves(), activeMobs.size(), progress);

        broadcast("§cВолна " + currentWave + "/" + config.getWaves() + " §e— на вас напало §c" + totalSpawned + " §eмонстров!");

        // Check completion every 2 seconds
        if (waveTask != null) waveTask.cancel();
        waveTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupDeadMobs();
                int remaining = activeMobs.size();
                double prog = remaining == 0 ? 0.0 : Math.max(0.05, (double) remaining / Math.max(1, totalSpawned));
                bossBar.showDuringRaid(currentWave, config.getWaves(), remaining, 1.0 - ((currentWave - 1.0 + (1.0 - prog)) / config.getWaves()));

                if (remaining <= 0) {
                    cancel();
                    broadcast("§aВолна " + currentWave + " очищена!");
                    // Short pause then next wave
                    Bukkit.getScheduler().runTaskLater(plugin, () -> startNextWave(), 60L);
                }
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }

    private LivingEntity spawnConfiguredMob(MobSettings ms, Location loc) {
        EntityType type;
        try {
            type = EntityType.valueOf(ms.getType());
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (!type.isAlive() || !type.isSpawnable()) return null;

        LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
        entity.setRemoveWhenFarAway(false);
        entity.setPersistent(true);

        // Basic equipment
        EntityEquipment eq = entity.getEquipment();
        if (eq != null) {
            Random r = ThreadLocalRandom.current();
            if (r.nextInt(100) < ms.getArmorChance()) {
                boolean incomplete = r.nextInt(100) < ms.getIncompleteArmorChance();
                Material[] armors = {Material.LEATHER_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.IRON_LEGGINGS, Material.GOLDEN_BOOTS};
                if (r.nextBoolean()) armors = new Material[]{Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS};

                if (!incomplete || r.nextBoolean()) eq.setHelmet(new ItemStack(armors[0]));
                if (!incomplete || r.nextBoolean()) eq.setChestplate(new ItemStack(armors[1]));
                if (!incomplete || r.nextBoolean()) eq.setLeggings(new ItemStack(armors[2]));
                if (!incomplete || r.nextBoolean()) eq.setBoots(new ItemStack(armors[3]));
            }
            if (r.nextInt(100) < ms.getWeaponChance()) {
                eq.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            }
            eq.setHelmetDropChance(0.05f);
            eq.setChestplateDropChance(0.05f);
            eq.setLeggingsDropChance(0.05f);
            eq.setBootsDropChance(0.05f);
            eq.setItemInMainHandDropChance(0.1f);
        }

        // Simple effect example
        if (ThreadLocalRandom.current().nextInt(100) < 15) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 0));
        }

        // Zombie block breaking is complex — placeholder:
        // For full implementation use Paper's Pathfinder or NMS / custom goals.
        // Here we just tag them.
        entity.addScoreboardTag("raiderworld_raid_mob");
        if (entity instanceof Zombie) {
            entity.addScoreboardTag("raiderworld_breaker"); // future AI handler
        }

        return entity;
    }

    private void spawnMiniBoss(Location center) {
        Location loc = getRandomSpawnLocation(center, 10);
        Zombie boss = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
        boss.setCustomName("§c§lРейд-Босс");
        boss.setCustomNameVisible(true);
        boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100);
        boss.setHealth(100);
        boss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12);
        boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
        boss.addScoreboardTag("raiderworld_raid_mob");
        activeMobs.add(boss.getUniqueId());
    }

    private Location getRandomSpawnLocation(Location center, int radius) {
        Random r = ThreadLocalRandom.current();
        for (int i = 0; i < 20; i++) {
            double angle = r.nextDouble() * 2 * Math.PI;
            double dist = 8 + r.nextDouble() * (radius - 8);
            double x = center.getX() + Math.cos(angle) * dist;
            double z = center.getZ() + Math.sin(angle) * dist;
            Location loc = new Location(center.getWorld(), x, center.getY(), z);
            loc.setY(center.getWorld().getHighestBlockYAt(loc) + 1);
            if (loc.getBlock().isPassable() && loc.clone().add(0, 1, 0).getBlock().isPassable()) {
                return loc;
            }
        }
        return center.clone().add(5, 0, 5);
    }

    private void cleanupDeadMobs() {
        activeMobs.removeIf(uuid -> {
            Entity e = Bukkit.getEntity(uuid);
            return e == null || e.isDead();
        });
    }

    public void endRaid(boolean success) {
        raidActive = false;
        if (waveTask != null) {
            waveTask.cancel();
            waveTask = null;
        }
        // Despawn remaining
        for (UUID uuid : new HashSet<>(activeMobs)) {
            Entity e = Bukkit.getEntity(uuid);
            if (e != null) e.remove();
        }
        activeMobs.clear();

        if (success) {
            bossBar.showFinished();
            broadcast("§a§l✓ РЕЙД УСПЕШНО ОТБИТ! §eВсе волны пройдены.");
        } else {
            bossBar.removeAll();
            broadcast("§cРейд прерван.");
        }
        nextRaidTicks = config.getIntervalTicks();
    }

    public boolean isRaidActive() {
        return raidActive;
    }

    public boolean canCancel() {
        return config.isAllowCancel();
    }

    public void shutdown() {
        if (schedulerTask != null) schedulerTask.cancel();
        if (waveTask != null) waveTask.cancel();
        endRaid(false);
    }

    private Player getRandomOnlinePlayer() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) return null;
        return players.get(ThreadLocalRandom.current().nextInt(players.size()));
    }

    private void broadcast(String msg) {
        Bukkit.broadcastMessage(msg);
    }

    public int getCurrentWave() { return currentWave; }
    public int getRemainingMobs() { return activeMobs.size(); }
}
