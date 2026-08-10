package ru.mineseason.teleportmenu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class TeleportMenu extends JavaPlugin implements Listener, TabExecutor {

    private static final String MAIN_MENU = "main";
    private static final String PLAYERS_MENU = "players";
    private static final String POSITION_MENU = "position";

    private final List<String> registeredPlayers = new ArrayList<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();

        loadPlayers();

        Bukkit.getPluginManager().registerEvents(this, this);

        if (getCommand("tpmenu") != null) {
            getCommand("tpmenu").setExecutor(this);
            getCommand("tpmenu").setTabCompleter(this);
        }

        getLogger().info("TeleportMenu v3.0.0 enabled!");
    }

    @Override
    public void onDisable() {
        savePlayers();
    }

    // =========================================================
    // CONFIG
    // =========================================================

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String message(String path, String... replacements) {

        String text = getConfig().getString(
                path,
                "&cMessage not configured: " + path
        );

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(
                    replacements[i],
                    replacements[i + 1]
            );
        }

        return color(text);
    }

    // =========================================================
    // PLAYERS
    // =========================================================

    private void loadPlayers() {

        registeredPlayers.clear();

        registeredPlayers.addAll(
                getConfig().getStringList(
                        "registered-players"
                )
        );
    }

    private void savePlayers() {

        getConfig().set(
                "registered-players",
                registeredPlayers
        );

        saveConfig();
    }

    // =========================================================
    // ITEMS
    // =========================================================

    private ItemStack createItem(
            Material material,
            String name
    ) {

        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(
                    color(name)
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createItem(
            Material material,
            String name,
            List<String> lore
    ) {

        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(
                    color(name)
            );

            List<String> coloredLore =
                    new ArrayList<>();

            for (String line : lore) {
                coloredLore.add(
                        color(line)
                );
            }

            meta.setLore(coloredLore);

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createPlayerHead(
            String playerName
    ) {

        ItemStack head =
                new ItemStack(Material.PLAYER_HEAD);

        SkullMeta meta =
                (SkullMeta) head.getItemMeta();

        if (meta != null) {

            Player target =
                    Bukkit.getPlayerExact(playerName);

            meta.setOwningPlayer(
                    Bukkit.getOfflinePlayer(playerName)
            );

            if (target != null &&
                    target.isOnline()) {

                meta.setDisplayName(
                        color(
                                "&a" +
                                playerName +
                                " &7• Онлайн"
                        )
                );

                meta.setLore(
                        List.of(
                                color(
                                        "&7Нажми, чтобы телепортироваться."
                                )
                        )
                );

            } else {

                meta.setDisplayName(
                        color(
                                "&c" +
                                playerName +
                                " &7• Не в сети"
                        )
                );

                meta.setLore(
                        List.of(
                                color(
                                        "&7Игрок сейчас не в сети."
                                )
                        )
                );
            }

            head.setItemMeta(meta);
        }

        return head;
    }

    // =========================================================
    // MAIN MENU
    // =========================================================

    private void openMainMenu(Player player) {

        String title = color(
                getConfig().getString(
                        "menu.title",
                        "&8Телепортация"
                )
        );

        int size = getConfig().getInt(
                "menu.size",
                27
        );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        size,
                        title
                );

        // Игроки
        inventory.setItem(
                11,
                createItem(
                        Material.PLAYER_HEAD,
                        "&b&lИгроки",
                        List.of(
                                "&7Телепортация к игрокам"
                        )
                )
        );

        // Позиции
        inventory.setItem(
                13,
                createItem(
                        Material.ENDER_PEARL,
                        "&e&lПозиции",
                        List.of(
                                "&7Сохранённая точка"
                        )
                )
        );

        // Спавн
        if (getConfig().getBoolean(
                "spawn.enabled",
                true
        )) {

            inventory.setItem(
                    15,
                    createItem(
                            Material.COMPASS,
                            getConfig().getString(
                                    "spawn.name",
                                    "&a&lСпавн"
                            ),
                            List.of(
                                    "&7Телепортироваться на спавн"
                            )
                    )
            );
        }

        // Назад / закрыть
        if (getConfig().getBoolean(
                "back.enabled",
                true
        )) {

            inventory.setItem(
                    26,
                    createItem(
                            Material.BARRIER,
                            getConfig().getString(
                                    "back.name",
                                    "&c&lНазад"
                            )
                    )
            );
        }

        player.openInventory(inventory);
    }

    // =========================================================
    // PLAYERS MENU
    // =========================================================

    private void openPlayersMenu(Player player) {

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        27,
                        color("&8Телепортация • Игроки")
                );

        int startSlot =
                getConfig().getInt(
                        "players.slot-start",
                        10
                );

        int endSlot =
                getConfig().getInt(
                        "players.slot-end",
                        16
                );

        int slot = startSlot;

        for (String playerName :
                registeredPlayers) {

            if (slot > endSlot) {
                break;
            }

            inventory.setItem(
                    slot,
                    createPlayerHead(playerName)
            );

            slot++;
        }

        // Назад
        inventory.setItem(
                26,
                createItem(
                        Material.BARRIER,
                        "&c&lНазад",
                        List.of(
                                "&7Вернуться в главное меню"
                        )
                )
        );

        player.openInventory(inventory);
    }

    // =========================================================
    // POSITION MENU
    // =========================================================

    private void openPositionMenu(Player player) {

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        27,
                        color("&8Телепортация • Позиции")
                );

        boolean pointExists =
                getSavedPoint() != null;

        if (pointExists) {

            inventory.setItem(
                    11,
                    createItem(
                            Material.ENDER_PEARL,
                            getConfig().getString(
                                    "saved-point.teleport-name",
                                    "&a&lТелепортироваться"
                            ),
                            List.of(
                                    "&7Телепортироваться",
                                    "&7в сохранённую точку"
                            )
                    )
            );

        } else {

            inventory.setItem(
                    11,
                    createItem(
                            Material.GRAY_DYE,
                            "&7&lТочка не установлена",
                            List.of(
                                    "&7Сначала установи точку"
                            )
                    )
            );
        }

        // Перезаписать
        inventory.setItem(
                15,
                createItem(
                        Material.WRITABLE_BOOK,
                        getConfig().getString(
                                "saved-point.overwrite-name",
                                "&6&lПерезаписать позицию"
                        ),
                        List.of(
                                "&7Сохранить твою текущую",
                                "&7позицию как новую точку"
                        )
                )
        );

        // Назад
        inventory.setItem(
                26,
                createItem(
                        Material.BARRIER,
                        "&c&lНазад",
                        List.of(
                                "&7Вернуться в главное меню"
                        )
                )
        );

        player.openInventory(inventory);
    }

    // =========================================================
    // SAVED LOCATION
    // =========================================================

    private Location getSavedPoint() {

        if (!getConfig().contains(
                "saved-location.world"
        )) {
            return null;
        }

        String worldName =
                getConfig().getString(
                        "saved-location.world"
                );

        if (worldName == null) {
            return null;
        }

        World world =
                Bukkit.getWorld(worldName);

        if (world == null) {
            return null;
        }

        double x =
                getConfig().getDouble(
                        "saved-location.x"
                );

        double y =
                getConfig().getDouble(
                        "saved-location.y"
                );

        double z =
                getConfig().getDouble(
                        "saved-location.z"
                );

        float yaw =
                (float) getConfig().getDouble(
                        "saved-location.yaw"
                );

        float pitch =
                (float) getConfig().getDouble(
                        "saved-location.pitch"
                );

        return new Location(
                world,
                x,
                y,
                z,
                yaw,
                pitch
        );
    }

    private void saveCurrentLocation(
            Player player
    ) {

        Location location =
                player.getLocation();

        getConfig().set(
                "saved-location.world",
                location.getWorld().getName()
        );

        getConfig().set(
                "saved-location.x",
                location.getX()
        );

        getConfig().set(
                "saved-location.y",
                location.getY()
        );

        getConfig().set(
                "saved-location.z",
                location.getZ()
        );

        getConfig().set(
                "saved-location.yaw",
                location.getYaw()
        );

        getConfig().set(
                "saved-location.pitch",
                location.getPitch()
        );

        saveConfig();
    }

    private void removeSavedLocation() {

        getConfig().set(
                "saved-location",
                null
        );

        saveConfig();
    }

    // =========================================================
    // TELEPORT ITEM
    // =========================================================

    private boolean isTeleportItem(
            ItemStack item
    ) {

        if (item == null) {
            return false;
        }

        Material material =
                Material.matchMaterial(
                        getConfig().getString(
                                "item.material",
                                "COMPASS"
                        )
                );

        if (material == null) {
            material = Material.COMPASS;
        }

        if (item.getType() != material) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null ||
                !meta.hasDisplayName()) {
            return false;
        }

        String requiredName =
                color(
                        getConfig().getString(
                                "item.name",
                                "&b&lТелепорт"
                        )
                );

        return meta.getDisplayName()
                .equals(requiredName);
    }

    @EventHandler
    public void onTeleportItemUse(
            PlayerInteractEvent event
    ) {

        if (!isTeleportItem(
                event.getItem()
        )) {
            return;
        }

        event.setCancelled(true);

        openMainMenu(
                event.getPlayer()
        );
    }

    // =========================================================
    // MENU CLICKS
    // =========================================================

    @EventHandler
    public void onMenuClick(
            InventoryClickEvent event
    ) {

        if (!(event.getWhoClicked()
                instanceof Player player)) {
            return;
        }

        String mainTitle =
                color(
                        getConfig().getString(
                                "menu.title",
                                "&8Телепортация"
                        )
                );

        String playersTitle =
                color("&8Телепортация • Игроки");

        String positionTitle =
                color("&8Телепортация • Позиции");

        String title =
                event.getView().getTitle();

        if (!title.equals(mainTitle) &&
                !title.equals(playersTitle) &&
                !title.equals(positionTitle)) {

            return;
        }

        event.setCancelled(true);

        int slot =
                event.getRawSlot();

        // =====================================================
        // MAIN MENU
        // =====================================================

        if (title.equals(mainTitle)) {

            // Игроки
            if (slot == 11) {

                openPlayersMenu(player);

                return;
            }

            // Позиции
            if (slot == 13) {

                openPositionMenu(player);

                return;
            }

            // Спавн
            if (slot == 15 &&
                    getConfig().getBoolean(
                            "spawn.enabled",
                            true
                    )) {

                World world =
                        player.getWorld();

                player.teleport(
                        world.getSpawnLocation()
                );

                player.sendMessage(
                        message(
                                "messages.teleported-spawn"
                        )
                );

                return;
            }

            // Закрыть
            if (slot == 26) {

                player.closeInventory();

                return;
            }
        }

        // =====================================================
        // PLAYERS MENU
        // =====================================================

        if (title.equals(playersTitle)) {

            if (slot == 26) {

                openMainMenu(player);

                return;
            }

            int startSlot =
                    getConfig().getInt(
                            "players.slot-start",
                            10
                    );

            int endSlot =
                    getConfig().getInt(
                            "players.slot-end",
                            16
                    );

            if (slot >= startSlot &&
                    slot <= endSlot) {

                int index =
                        slot - startSlot;

                if (index < 0 ||
                        index >=
                                registeredPlayers.size()) {
                    return;
                }

                String targetName =
                        registeredPlayers.get(index);

                Player target =
                        Bukkit.getPlayerExact(
                                targetName
                        );

                if (target == null ||
                        !target.isOnline()) {

                    player.sendMessage(
                            message(
                                    "messages.player-offline",
                                    "%player%",
                                    targetName
                            )
                    );

                    return;
                }

                // Телепортация к самому себе
                // специально разрешена для тестирования.

                player.teleport(
                        target.getLocation()
                );

                player.closeInventory();

                player.sendMessage(
                        message(
                                "messages.teleported",
                                "%player%",
                                targetName
                        )
                );
            }
        }

        // =====================================================
        // POSITION MENU
        // =====================================================

        if (title.equals(positionTitle)) {

            // Телепортироваться
            if (slot == 11) {

                Location savedPoint =
                        getSavedPoint();

                if (savedPoint == null) {

                    player.sendMessage(
                            message(
                                    "messages.saved-point-not-set"
                            )
                    );

                    return;
                }

                player.teleport(
                        savedPoint
                );

                player.closeInventory();

                player.sendMessage(
                        message(
                                "messages.saved-point-teleported"
                        )
                );

                return;
            }

            // Перезаписать
            if (slot == 15) {

                saveCurrentLocation(player);

                player.sendMessage(
                        message(
                                "messages.saved-point-overwritten"
                        )
                );

                openPositionMenu(player);

                return;
            }

            // Назад
            if (slot == 26) {

                openMainMenu(player);

                return;
            }
        }
    }

    // =========================================================
    // GIVE TELEPORT ITEM
    // =========================================================

    private void giveTeleportItem(
            Player player
    ) {

        Material material =
                Material.matchMaterial(
                        getConfig().getString(
                                "item.material",
                                "COMPASS"
                        )
                );

        if (material == null) {
            material = Material.COMPASS;
        }

        String name =
                getConfig().getString(
                        "item.name",
                        "&b&lТелепорт"
                );

        ItemStack item =
                createItem(
                        material,
                        name
                );

        player.getInventory().addItem(item);

        player.sendMessage(
                color(
                        "&aПредмет телепортации выдан."
                )
        );
    }

    // =========================================================
    // COMMANDS
    // =========================================================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    "Only players can use this command."
            );

            return true;
        }

        // /tpmenu
        if (args.length == 0) {

            openMainMenu(player);

            return true;
        }

        String subCommand =
                args[0].toLowerCase();

        // =====================================================
        // ADD PLAYER
        // =====================================================

        if (subCommand.equals(
                getConfig().getString(
                        "commands.add-player",
                        "addplayer"
                )
        )) {

            if (!player.hasPermission(
                    "teleportmenu.admin"
            )) {

                player.sendMessage(
                        message(
                                "messages.no-permission"
                        )
                );

                return true;
            }

            if (args.length < 2) {

                player.sendMessage(
                        color(
                                "&cИспользование: /tpmenu addplayer <ник>"
                        )
                );

                return true;
            }

            String playerName =
                    args[1];

            Player target =
                    Bukkit.getPlayerExact(
                            playerName
                    );

            // Игрок должен быть онлайн
            if (target == null ||
                    !target.isOnline()) {

                player.sendMessage(
                        message(
                                "messages.player-not-found",
                                "%player%",
                                playerName
                        )
                );

                return true;
            }

            if (registeredPlayers.contains(
                    target.getName()
            )) {

                player.sendMessage(
                        message(
                                "messages.player-already-added",
                                "%player%",
                                target.getName()
                        )
                );

                return true;
            }

            registeredPlayers.add(
                    target.getName()
            );

            savePlayers();

            player.sendMessage(
                    message(
                            "messages.player-added",
                            "%player%",
                            target.getName()
                    )
            );

            return true;
        }

        // =====================================================
        // REMOVE PLAYER
        // =====================================================

        if (subCommand.equals(
                getConfig().getString(
                        "commands.remove-player",
                        "removeplayer"
                )
        )) {

            if (!player.hasPermission(
                    "teleportmenu.admin"
            )) {

                player.sendMessage(
                        message(
                                "messages.no-permission"
                        )
                );

                return true;
            }

            if (args.length < 2) {

                player.sendMessage(
                        color(
                                "&cИспользование: /tpmenu removeplayer <ник>"
                        )
                );

                return true;
            }

            String playerName =
                    args[1];

            if (!registeredPlayers.remove(
                    playerName
            )) {

                player.sendMessage(
                        color(
                                "&cЭтот игрок не добавлен в меню."
                        )
                );

                return true;
            }

            savePlayers();

            player.sendMessage(
                    message(
                            "messages.player-removed",
                            "%player%",
                            playerName
                    )
            );

            return true;
        }

        // =====================================================
        // LIST
        // =====================================================

        if (subCommand.equals(
                getConfig().getString(
                        "commands.list-players",
                        "list"
                )
        )) {

            player.sendMessage(
                    color("&b&lИгроки в меню:")
            );

            if (registeredPlayers.isEmpty()) {

                player.sendMessage(
                        color("&7Список пуст.")
                );

                return true;
            }

            for (String playerName :
                    registeredPlayers) {

                Player target =
                        Bukkit.getPlayerExact(
                                playerName
                        );

                String status =
                        target != null &&
                        target.isOnline()
                                ? "&aонлайн"
                                : "&cоффлайн";

                player.sendMessage(
                        color(
                                "&7• &f" +
                                playerName +
                                " &7— " +
                                status
                        )
                );
            }

            return true;
        }

        // =====================================================
        // GIVE ITEM
        // =====================================================

        if (subCommand.equals(
                getConfig().getString(
                        "commands.give-item",
                        "item"
                )
        )) {

            if (!player.hasPermission(
                    "teleportmenu.admin"
            )) {

                player.sendMessage(
                        message(
                                "messages.no-permission"
                        )
                );

                return true;
            }

            giveTeleportItem(player);

            return true;
        }

        // =====================================================
        // SET POINT
        // =====================================================

        if (subCommand.equals(
                getConfig().getString(
                        "commands.set-point",
                        "setpoint"
                )
        )) {

            if (!player.hasPermission(
                    "teleportmenu.admin"
            )) {

                player.sendMessage(
                        message(
                                "messages.no-permission"
                        )
                );

                return true;
            }

            saveCurrentLocation(player);

            player.sendMessage(
                    message(
                            "messages.saved-point-overwritten"
                    )
            );

            return true;
        }

        // =====================================================
        // REMOVE POINT
        // =====================================================

        if (subCommand.equals(
                getConfig().getString(
                        "commands.remove-point",
                        "removepoint"
                )
        )) {

            if (!player.hasPermission(
                    "teleportmenu.admin"
            )) {

                player.sendMessage(
                        message(
                                "messages.no-permission"
                        )
                );

                return true;
            }

            removeSavedLocation();

            player.sendMessage(
                    color(
                            "&aСохранённая точка удалена."
                    )
            );

            return true;
        }

        // =====================================================
        // UNKNOWN COMMAND
        // =====================================================

        player.sendMessage(
                color(
                        "&cНеизвестная команда. Используй /tpmenu"
                )
        );

        return true;
    }

    // =========================================================
    // TAB COMPLETE
    // =========================================================

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        List<String> result =
                new ArrayList<>();

        if (args.length == 1) {

            result.add(
                    getConfig().getString(
                            "commands.add-player",
                            "addplayer"
                    )
            );

            result.add(
                    getConfig().getString(
                            "commands.remove-player",
                            "removeplayer"
                    )
            );

            result.add(
                    getConfig().getString(
                            "commands.list-players",
                            "list"
                    )
            );

            result.add(
                    getConfig().getString(
                            "commands.give-item",
                            "item"
                    )
            );

            result.add(
                    getConfig().getString(
                            "commands.set-point",
                            "setpoint"
                    )
            );

            result.add(
                    getConfig().getString(
                            "commands.remove-point",
                            "removepoint"
                    )
            );
        }

        return result;
    }
}