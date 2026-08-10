package com.raiderworld.commands;

import com.raiderworld.RaiderWorldPlugin;
import com.raiderworld.managers.ConfigManager;
import com.raiderworld.managers.RaidManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RaidCommand implements CommandExecutor, TabCompleter {

    private final RaiderWorldPlugin plugin;

    public RaidCommand(RaiderWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("raiderworld.admin") && !sender.hasPermission("raiderworld.start")) {
            sender.sendMessage("§cНет прав.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        RaidManager raid = plugin.getRaidManager();
        ConfigManager cfg = plugin.getConfigManager();

        switch (sub) {
            case "start" -> {
                if (args.length >= 2) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage("§cИгрок не найден.");
                        return true;
                    }
                    raid.startRaidNearPlayer(target);
                    sender.sendMessage("§aРейд запущен около " + target.getName());
                } else if (sender instanceof Player p) {
                    raid.startRaidNearPlayer(p);
                    sender.sendMessage("§aРейд запущен около вас.");
                } else {
                    sender.sendMessage("§cУкажите игрока: /raid start <player>");
                }
            }
            case "stop", "cancel" -> {
                if (!raid.isRaidActive()) {
                    sender.sendMessage("§cСейчас нет активного рейда.");
                    return true;
                }
                if (!raid.canCancel() && !sender.hasPermission("raiderworld.admin")) {
                    sender.sendMessage("§cОтмена рейда запрещена настройками.");
                    return true;
                }
                raid.endRaid(false);
                sender.sendMessage("§eРейд остановлен.");
            }
            case "setspawn" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("§cТолько игрок.");
                    return true;
                }
                Location loc = p.getLocation();
                cfg.setSpawn(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
                sender.sendMessage("§aТочка рейда установлена: " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
            }
            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage("§aКонфиг перезагружен.");
            }
            case "status" -> {
                if (raid.isRaidActive()) {
                    sender.sendMessage("§eРейд активен. Волна: " + raid.getCurrentWave() + ", мобов: " + raid.getRemainingMobs());
                } else {
                    sender.sendMessage("§aРейд не активен. Следующий через настройки interval.");
                }
            }
            case "settings" -> handleSettings(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleSettings(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§e/raid settings <waves|interval|night|radius|difficulty|mobs|allowcancel> ...");
            return;
        }
        ConfigManager cfg = plugin.getConfigManager();
        String key = args[1].toLowerCase();

        try {
            switch (key) {
                case "waves" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eТекущие волны: " + cfg.getWaves());
                        return;
                    }
                    int w = Integer.parseInt(args[2]);
                    cfg.setWaves(w);
                    sender.sendMessage("§aКоличество волн установлено: " + w);
                }
                case "interval" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eИспользование: /raid settings interval <дни>");
                        return;
                    }
                    // support 5d or just number
                    String val = args[2].toLowerCase().replace("d", "");
                    int days = Integer.parseInt(val);
                    cfg.setIntervalDays(days);
                    sender.sendMessage("§aИнтервал рейдов: каждые " + days + " дней.");
                }
                case "night" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eforce-night: " + cfg.isForceNight());
                        return;
                    }
                    boolean v = Boolean.parseBoolean(args[2]);
                    cfg.setForceNight(v);
                    sender.sendMessage("§aForce night: " + v);
                }
                case "radius" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eРадиус: " + cfg.getRadius());
                        return;
                    }
                    int r = Integer.parseInt(args[2]);
                    cfg.setRadius(r);
                    sender.sendMessage("§aРадиус спавна: " + r);
                }
                case "difficulty" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eСложность: " + cfg.getDifficulty());
                        return;
                    }
                    cfg.setDifficulty(args[2].toUpperCase());
                    sender.sendMessage("§aСложность: " + args[2]);
                }
                case "allowcancel" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eallow-cancel: " + cfg.isAllowCancel());
                        return;
                    }
                    boolean v = Boolean.parseBoolean(args[2]);
                    cfg.setAllowCancel(v);
                    sender.sendMessage("§aРазрешить отмену: " + v);
                }
                case "mobs" -> handleMobs(sender, args);
                default -> sender.sendMessage("§cНеизвестный параметр settings.");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cНеверное число.");
        }
    }

    private void handleMobs(CommandSender sender, String[] args) {
        // /raid settings mobs add <type> <minWave> <min-max>
        // /raid settings mobs remove <type>
        // /raid settings mobs armor <type> <chance>
        // /raid settings mobs weapon <type> <chance>
        if (args.length < 3) {
            sender.sendMessage("§e/raid settings mobs <add|remove|armor|weapon|list> ...");
            return;
        }
        ConfigManager cfg = plugin.getConfigManager();
        String action = args[2].toLowerCase();

        switch (action) {
            case "list" -> {
                sender.sendMessage("§6=== Настроенные мобы ===");
                cfg.getMobSettings().forEach((type, ms) -> {
                    sender.sendMessage("§e" + type + " §7| wave≥" + ms.getMinWave() +
                            " count " + ms.getMinCount() + "-" + ms.getMaxCount() +
                            " armor%" + ms.getArmorChance() + " weapon%" + ms.getWeaponChance());
                });
            }
            case "add" -> {
                if (args.length < 6) {
                    sender.sendMessage("§e/raid settings mobs add <type> <minWave> <minCount-maxCount>");
                    sender.sendMessage("§7Пример: /raid settings mobs add skeleton 1 15-30");
                    return;
                }
                String type = args[3];
                int minWave = Integer.parseInt(args[4]);
                String range = args[5];
                String[] parts = range.split("-");
                int minC = Integer.parseInt(parts[0]);
                int maxC = parts.length > 1 ? Integer.parseInt(parts[1]) : minC;
                cfg.addOrUpdateMob(type, minWave, minC, maxC);
                sender.sendMessage("§aМоб " + type + " добавлен/обновлён: wave≥" + minWave + " count " + minC + "-" + maxC);
            }
            case "remove" -> {
                if (args.length < 4) {
                    sender.sendMessage("§e/raid settings mobs remove <type>");
                    return;
                }
                cfg.removeMob(args[3]);
                sender.sendMessage("§aМоб " + args[3] + " удалён.");
            }
            case "armor" -> {
                if (args.length < 5) {
                    sender.sendMessage("§e/raid settings mobs armor <type> <chance 0-100>");
                    return;
                }
                cfg.setMobArmorChance(args[3], Integer.parseInt(args[4]));
                sender.sendMessage("§aШанс брони для " + args[3] + ": " + args[4] + "%");
            }
            case "weapon" -> {
                if (args.length < 5) {
                    sender.sendMessage("§e/raid settings mobs weapon <type> <chance 0-100>");
                    return;
                }
                cfg.setMobWeaponChance(args[3], Integer.parseInt(args[4]));
                sender.sendMessage("§aШанс оружия для " + args[3] + ": " + args[4] + "%");
            }
            default -> sender.sendMessage("§cНеизвестное действие для mobs.");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== RaiderWorld ===");
        sender.sendMessage("§e/raid start [player] §7— запустить рейд");
        sender.sendMessage("§e/raid stop §7— остановить (если разрешено)");
        sender.sendMessage("§e/raid setspawn §7— установить фиксированную точку");
        sender.sendMessage("§e/raid status §7— статус");
        sender.sendMessage("§e/raid reload §7— перезагрузить конфиг");
        sender.sendMessage("§e/raid settings waves <n>");
        sender.sendMessage("§e/raid settings interval <дни>");
        sender.sendMessage("§e/raid settings night <true|false>");
        sender.sendMessage("§e/raid settings radius <n>");
        sender.sendMessage("§e/raid settings difficulty <HARD|...> ");
        sender.sendMessage("§e/raid settings mobs add <type> <wave> <min-max>");
        sender.sendMessage("§e/raid settings mobs remove <type>");
        sender.sendMessage("§e/raid settings mobs armor|weapon <type> <chance>");
        sender.sendMessage("§e/raid settings mobs list");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("start", "stop", "setspawn", "status", "reload", "settings"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("settings")) {
            completions.addAll(Arrays.asList("waves", "interval", "night", "radius", "difficulty", "mobs", "allowcancel"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("settings") && args[1].equalsIgnoreCase("mobs")) {
            completions.addAll(Arrays.asList("add", "remove", "armor", "weapon", "list"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return null; // players
        }
        String last = args[args.length - 1].toLowerCase();
        return completions.stream().filter(s -> s.toLowerCase().startsWith(last)).collect(Collectors.toList());
    }
}
