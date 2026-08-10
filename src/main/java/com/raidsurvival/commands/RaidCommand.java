package com.raidsurvival.commands;

import com.raidsurvival.RaidSurvivalPlugin;
import com.raidsurvival.managers.ConfigManager;
import com.raidsurvival.managers.RaidManager;
import org.bukkit.Bukkit;
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

    private final RaidSurvivalPlugin plugin;

    public RaidCommand(RaidSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("raidsurvival.admin") && !sender.hasPermission("raidsurvival.start")) {
            sender.sendMessage("§cНет прав.");
            return true;
        }

        if (args.length == 0) {
            help(sender);
            return true;
        }

        RaidManager raid = plugin.getRaidManager();
        ConfigManager cfg = plugin.getConfigManager();
        String sub = args[0].toLowerCase();

        switch (sub) {
            case "start" -> {
                if (args.length >= 2) {
                    Player t = Bukkit.getPlayer(args[1]);
                    if (t == null) {
                        sender.sendMessage("§cИгрок не найден.");
                        return true;
                    }
                    raid.startRaid(t.getLocation());
                    sender.sendMessage("§aРейд запущен около " + t.getName());
                } else if (sender instanceof Player p) {
                    raid.startRaid(p.getLocation());
                    sender.sendMessage("§aРейд запущен.");
                } else {
                    sender.sendMessage("§cУкажи игрока.");
                }
            }
            case "stop" -> {
                if (!raid.isActive()) {
                    sender.sendMessage("§cРейд не идёт.");
                    return true;
                }
                if (!raid.canCancel()) {
                    sender.sendMessage("§cОтмена запрещена настройками.");
                    return true;
                }
                raid.stopRaid();
                sender.sendMessage("§eРейд остановлен.");
            }
            case "status" -> {
                sender.sendMessage(raid.isActive() ? "§eРейд активен." : "§aРейд не активен.");
            }
            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage("§aКонфиг перезагружен.");
            }
            case "settings" -> settings(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void settings(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§e/raid settings <waves|interval|night|radius|preventsleep|allowcancel|mobs|rewardxp>");
            return;
        }
        ConfigManager cfg = plugin.getConfigManager();
        String key = args[1].toLowerCase();

        try {
            switch (key) {
                case "waves" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eВолны: " + cfg.getWaves());
                        return;
                    }
                    cfg.setWaves(Integer.parseInt(args[2]));
                    sender.sendMessage("§aВолны: " + args[2]);
                }
                case "interval" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eИспользование: /raid settings interval <дни>");
                        return;
                    }
                    int d = Integer.parseInt(args[2].replace("d", ""));
                    cfg.setIntervalDays(d);
                    sender.sendMessage("§aИнтервал: " + d + " дней");
                }
                case "night" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eforce-night: " + cfg.isForceNight());
                        return;
                    }
                    cfg.setForceNight(Boolean.parseBoolean(args[2]));
                    sender.sendMessage("§aНочь во время рейда: " + args[2]);
                }
                case "radius" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eРадиус: " + cfg.getRadius());
                        return;
                    }
                    cfg.setRadius(Integer.parseInt(args[2]));
                    sender.sendMessage("§aРадиус: " + args[2]);
                }
                case "preventsleep" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eprevent-sleep: " + cfg.isPreventSleep());
                        return;
                    }
                    cfg.setPreventSleep(Boolean.parseBoolean(args[2]));
                    sender.sendMessage("§aЗапрет сна: " + args[2]);
                }
                case "allowcancel" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eallow-cancel: " + cfg.isAllowCancel());
                        return;
                    }
                    cfg.setAllowCancel(Boolean.parseBoolean(args[2]));
                    sender.sendMessage("§aМожно отменять: " + args[2]);
                }
                case "rewardxp" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eXP награда: " + cfg.getRewardXp());
                        return;
                    }
                    cfg.setRewardXp(Integer.parseInt(args[2]));
                    sender.sendMessage("§aXP награда: " + args[2]);
                }
                case "mobs" -> mobs(sender, args);
                default -> sender.sendMessage("§cНеизвестный параметр.");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cНеверное число.");
        }
    }

    private void mobs(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§e/raid settings mobs <add|remove|armor|list>");
            return;
        }
        ConfigManager cfg = plugin.getConfigManager();
        String act = args[2].toLowerCase();

        switch (act) {
            case "list" -> {
                sender.sendMessage("§6=== Мобы ===");
                cfg.getMobSettings().forEach((t, m) ->
                        sender.sendMessage("§e" + t + " §7wave≥" + m.getMinWave() +
                                " " + m.getMinCount() + "-" + m.getMaxCount() +
                                " armor%" + m.getArmorChance()));
            }
            case "add" -> {
                if (args.length < 6) {
                    sender.sendMessage("§e/raid settings mobs add <TYPE> <minWave> <min-max>");
                    return;
                }
                String type = args[3];
                int wave = Integer.parseInt(args[4]);
                String[] range = args[5].split("-");
                int min = Integer.parseInt(range[0]);
                int max = range.length > 1 ? Integer.parseInt(range[1]) : min;
                cfg.addOrUpdateMob(type, wave, min, max);
                sender.sendMessage("§aМоб " + type + " добавлен.");
            }
            case "remove" -> {
                if (args.length < 4) {
                    sender.sendMessage("§e/raid settings mobs remove <TYPE>");
                    return;
                }
                cfg.removeMob(args[3]);
                sender.sendMessage("§aМоб удалён.");
            }
            case "armor" -> {
                if (args.length < 5) {
                    sender.sendMessage("§e/raid settings mobs armor <TYPE> <0-100>");
                    return;
                }
                cfg.setMobArmor(args[3], Integer.parseInt(args[4]));
                sender.sendMessage("§aШанс брони обновлён.");
            }
            default -> sender.sendMessage("§cНеизвестное действие.");
        }
    }

    private void help(CommandSender s) {
        s.sendMessage("§6=== RaidSurvival ===");
        s.sendMessage("§e/raid start [игрок] §7— запустить рейд");
        s.sendMessage("§e/raid stop §7— остановить (если разрешено)");
        s.sendMessage("§e/raid status");
        s.sendMessage("§e/raid reload");
        s.sendMessage("§e/raid settings waves <n>");
        s.sendMessage("§e/raid settings interval <дни>");
        s.sendMessage("§e/raid settings night true/false");
        s.sendMessage("§e/raid settings radius <n>");
        s.sendMessage("§e/raid settings preventsleep true/false");
        s.sendMessage("§e/raid settings allowcancel true/false");
        s.sendMessage("§e/raid settings rewardxp <число>");
        s.sendMessage("§e/raid settings mobs add <TYPE> <wave> <min-max>");
        s.sendMessage("§e/raid settings mobs list / remove / armor");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.addAll(Arrays.asList("start", "stop", "status", "reload", "settings"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("settings")) {
            list.addAll(Arrays.asList("waves", "interval", "night", "radius", "preventsleep", "allowcancel", "mobs", "rewardxp"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("settings") && args[1].equalsIgnoreCase("mobs")) {
            list.addAll(Arrays.asList("add", "remove", "armor", "weapon", "list"));
        }
        String last = args[args.length - 1].toLowerCase();
        return list.stream().filter(s -> s.startsWith(last)).collect(Collectors.toList());
    }
}
