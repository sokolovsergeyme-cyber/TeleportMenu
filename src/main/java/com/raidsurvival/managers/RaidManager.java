package com.raidsurvival.managers;

import com.raidsurvival.RaidSurvivalPlugin;
import com.raidsurvival.models.MobSettings;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
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

    private final RaidSurvivalPlugin plugin;
    private final ConfigManager config;
    private final BossBarManager bossBar;

    private boolean active = false;
    private int currentWave = 0;
    private final Set<UUID> mobs = new HashSet<>();
    private Location center;
    private World world;
    private BukkitTask scheduler;
    private BukkitTask checker;
    private BukkitTask nightKeeper;
    private long nextRaidTicks;

    public RaidManager(RaidSurvivalPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.bossBar = plugin.getBossBarManager();
        this.nextRaidTicks = config.getIntervalTicks();
    }

    public void startScheduler() {
        scheduler = new BukkitRunnable() {
            @Override
            public void run() {
                if (active) return;
                nextRaidTicks -= 20;
                if (nextRaidTicks <= 0) {
                    Player p = getRandomPlayer();
                    if (p != null) {
                        startRaid(p.getLocation());
                    }
                    nextRaidTicks = config.getIntervalTicks();
                } else {
                    bossBar.showWaiting(nextRaidTicks);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void startRaid(Location loc) {
        if (active) return;
        active = true;
        currentWave = 0;
        center = loc.clone();
        world = loc.getWorld();
        mobs.clear();

        if (config.isForceNight()) {
            world.setTime(18000);
            startNightKeeper();
        }

        Bukkit.broadcastMessage("§c§l⚔ РЕЙД НАЧАЛСЯ! §eНельзя спать. Готовьтесь!");
        startNextWave();
    }

    private void startNightKeeper() {
        if (nightKeeper != null) {
            nightKeeper.cancel();
        }
        nightKeeper = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || world == null || !config.isForceNight()) {
                    cancel();
                    return;
                }
                world.setTime(18000);
            }
        }.runTaskTimer(plugin, 0L, 100L);
    }

    private void startNextWave() {
        currentWave++;
        if (currentWave > config.getWaves()) {
            finishRaid(true);
            return;
        }

        mobs.clear();
        int spawned = 0;

        for (MobSettings ms : config.getMobSettings().values()) {
            if (ms.getMinWave() > currentWave) continue;
            int count = ThreadLocalRandom.current().nextInt(ms.getMinCount(), ms.getMaxCount() + 1);
            for (int i = 0; i < count; i++) {
                LivingEntity e = spawnMob(ms, randomLoc());
                if (e != null) {
                    mobs.add(e.getUniqueId());
                    spawned++;
                }
            }
        }

        // Мини-босс на последней волне
        if (currentWave == config.getWaves()) {
            spawnMiniBoss();
        }

        bossBar.showRaid(currentWave, config.getWaves(), mobs.size());
        Bukkit.broadcastMessage("§cВолна " + currentWave + "/" + config.getWaves() + " §e— мобов: §c" + spawned);

        if (checker != null) checker.cancel();
        checker = new BukkitRunnable() {
            @Override
            public void run() {
                cleanDead();
                int left = mobs.size();
                bossBar.showRaid(currentWave, config.getWaves(), left);
                if (left <= 0) {
                    cancel();
                    Bukkit.broadcastMessage("§aВолна " + currentWave + " очищена!");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> startNextWave(), 80L);
                }
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }

    private LivingEntity spawnMob(MobSettings ms, Location loc) {
        EntityType type;
        try {
            type = EntityType.valueOf(ms.getType());
        } catch (Exception e) {
            return null;
        }
        if (!type.isAlive()) return null;

        LivingEntity ent = (LivingEntity) world.spawnEntity(loc, type);
        ent.setRemoveWhenFarAway(false);
        ent.setPersistent(true);
        ent.addScoreboardTag("raidsurvival_mob");

        EntityEquipment eq = ent.getEquipment();
        if (eq != null) {
            Random r = ThreadLocalRandom.current();
            if (r.nextInt(100) < ms.getArmorChance()) {
                eq.setHelmet(new ItemStack(Material.IRON_HELMET));
                eq.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                eq.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                eq.setBoots(new ItemStack(Material.IRON_BOOTS));
                // Иногда неполный комплект
                if (r.nextInt(100) < 35) {
                    if (r.nextBoolean()) eq.setHelmet(null);
                    if (r.nextBoolean()) eq.setBoots(null);
                }
            }
            eq.setHelmetDropChance(0.08f);
            eq.setChestplateDropChance(0.08f);
            eq.setItemInMainHandDropChance(0.12f);
        }

        // Немного усиления
        if (ThreadLocalRandom.current().nextInt(100) < 20) {
            ent.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 120, 0));
        }

        return ent;
    }

    private void spawnMiniBoss() {
        Location loc = randomLoc();
        Ravager boss = (Ravager) world.spawnEntity(loc, EntityType.RAVAGER);
        boss.setCustomName("§c§lРейд-Босс");
        boss.setCustomNameVisible(true);

        var maxHealth = boss.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(120.0);
            boss.setHealth(Math.min(120.0, maxHealth.getValue()));
        }
        boss.addScoreboardTag("raidsurvival_mob");
        mobs.add(boss.getUniqueId());
    }

    private Location randomLoc() {
        Random r = ThreadLocalRandom.current();
        for (int i = 0; i < 15; i++) {
            double angle = r.nextDouble() * Math.PI * 2;
            int radius = Math.max(10, config.getRadius());
            double dist = 10 + r.nextDouble() * (radius - 10);
            double x = center.getX() + Math.cos(angle) * dist;
            double z = center.getZ() + Math.sin(angle) * dist;
            Location l = new Location(world, x, center.getY(), z);
            l.setY(world.getHighestBlockYAt(l) + 1);
            if (l.getBlock().isPassable()) return l;
        }
        return center.clone().add(8, 0, 8);
    }

    private void cleanDead() {
        mobs.removeIf(uuid -> {
            Entity e = Bukkit.getEntity(uuid);
            return e == null || e.isDead();
        });
    }

    private void finishRaid(boolean success) {
        active = false;
        if (checker != null) checker.cancel();
        if (nightKeeper != null) {
            nightKeeper.cancel();
            nightKeeper = null;
        }
        for (UUID id : new HashSet<>(mobs)) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        mobs.clear();

        if (success) {
            bossBar.showFinished();
            Bukkit.broadcastMessage("§a§l✓ РЕЙД ОТБИТ! §eНаграда выдана всем онлайн игрокам.");
            giveRewards();
        } else {
            bossBar.removeAll();
            Bukkit.broadcastMessage("§cРейд прерван.");
        }
        nextRaidTicks = config.getIntervalTicks();
    }

    private void giveRewards() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.giveExp(config.getRewardXp());
            for (ItemStack item : config.getRewardItems()) {
                p.getInventory().addItem(item.clone());
            }
            p.sendMessage("§aНаграда: §e" + config.getRewardXp() + " XP §a+ предметы");
        }
    }

    public void stopRaid() {
        if (!active) return;
        if (!config.isAllowCancel()) return;
        finishRaid(false);
    }

    public boolean isActive() { return active; }
    public boolean canCancel() { return config.isAllowCancel(); }

    public void shutdown() {
        if (scheduler != null) scheduler.cancel();
        if (checker != null) checker.cancel();
        if (active) finishRaid(false);
    }

    private Player getRandomPlayer() {
        List<Player> list = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (list.isEmpty()) return null;
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
}
