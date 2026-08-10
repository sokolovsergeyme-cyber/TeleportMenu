package ru.mineseason.teleportmenu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class TeleportMenu extends JavaPlugin implements Listener {

    private NamespacedKey teleportItemKey;

    private static final String MENU_TITLE =
            ChatColor.DARK_AQUA + "Телепортация";

    @Override
    public void onEnable() {

        saveDefaultConfig();

        teleportItemKey =
                new NamespacedKey(this, "teleport_item");

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("TeleportMenu включён!");
    }

    @Override
    public void onDisable() {

        getLogger().info("TeleportMenu выключен!");
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    "Эту команду может использовать только игрок."
            );

            return true;
        }

        giveTeleportItem(player);

        player.sendMessage(
                ChatColor.GREEN +
                "Ты получил предмет телепортации."
        );

        return true;
    }

    private void giveTeleportItem(Player player) {

        ItemStack item =
                new ItemStack(Material.COMPASS);

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return;
        }

        meta.setDisplayName(
                ChatColor.AQUA +
                "Телепортация"
        );

        List<String> lore =
                new ArrayList<>();

        lore.add(
                ChatColor.GRAY +
                "Используй предмет,"
        );

        lore.add(
                ChatColor.GRAY +
                "чтобы открыть меню."
        );

        meta.setLore(lore);

        meta.getPersistentDataContainer().set(
                teleportItemKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);

        player.getInventory().addItem(item);
    }

    @EventHandler
    public void onPlayerInteract(
            PlayerInteractEvent event) {

        Action action =
                event.getAction();

        if (action != Action.RIGHT_CLICK_AIR &&
                action != Action.RIGHT_CLICK_BLOCK) {

            return;
        }

        ItemStack item =
                event.getItem();

        if (!isTeleportItem(item)) {
            return;
        }

        event.setCancelled(true);

        openTeleportMenu(
                event.getPlayer()
        );
    }

    private boolean isTeleportItem(
            ItemStack item) {

        if (item == null ||
                item.getType() != Material.COMPASS) {

            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return false;
        }

        return meta
                .getPersistentDataContainer()
                .has(
                        teleportItemKey,
                        PersistentDataType.BYTE
                );
    }

    private void openTeleportMenu(
            Player player) {

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        27,
                        MENU_TITLE
                );

        List<String> players =
                getConfig()
                        .getStringList("players");

        int[] slots = {
                11,
                13,
                15
        };

        for (
                int i = 0;
                i < players.size() && i < 3;
                i++
        ) {

            String playerName =
                    players.get(i);

            ItemStack head =
                    new ItemStack(
                            Material.PLAYER_HEAD
                    );

            ItemMeta meta =
                    head.getItemMeta();

            if (meta == null) {
                continue;
            }

            meta.setDisplayName(
                    ChatColor.YELLOW +
                    playerName
            );

            List<String> lore =
                    new ArrayList<>();

            Player target =
                    Bukkit.getPlayerExact(
                            playerName
                    );

            if (target != null) {

                lore.add(
                        ChatColor.GREEN +
                        "● В сети"
                );

                lore.add("");

                lore.add(
                        ChatColor.GRAY +
                        "Нажми, чтобы телепортироваться."
                );

            } else {

                lore.add(
                        ChatColor.RED +
                        "● Не в сети"
                );
            }

            meta.setLore(lore);

            head.setItemMeta(meta);

            inventory.setItem(
                    slots[i],
                    head
            );
        }

        ItemStack back =
                new ItemStack(
                        Material.BARRIER
                );

        ItemMeta backMeta =
                back.getItemMeta();

        if (backMeta != null) {

            backMeta.setDisplayName(
                    ChatColor.RED +
                    "Назад"
            );

            back.setItemMeta(backMeta);
        }

        inventory.setItem(
                22,
                back
        );

        player.openInventory(
                inventory
        );
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event) {

        if (!event.getView()
                .getTitle()
                .equals(MENU_TITLE)) {

            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player player)) {

            return;
        }

        int slot =
                event.getRawSlot();

        if (slot == 22) {

            player.closeInventory();

            return;
        }

        int[] slots = {
                11,
                13,
                15
        };

        if (!contains(slots, slot)) {
            return;
        }

        int index =
                indexOf(slots, slot);

        List<String> players =
                getConfig()
                        .getStringList("players");

        if (index >= players.size()) {
            return;
        }

        String targetName =
                players.get(index);

        Player target =
                Bukkit.getPlayerExact(
                        targetName
                );

        if (target == null) {

            player.sendMessage(
                    ChatColor.RED +
                    "Игрок " +
                    targetName +
                    " сейчас не в сети."
            );

            return;
        }

        player.closeInventory();

        player.teleport(target);

        player.sendMessage(
                ChatColor.GREEN +
                "Ты телепортировался к " +
                targetName +
                "!"
        );
    }

    private boolean contains(
            int[] array,
            int value) {

        for (int number : array) {

            if (number == value) {
                return true;
            }
        }

        return false;
    }

    private int indexOf(
            int[] array,
            int value) {

        for (
                int i = 0;
                i < array.length;
                i++
        ) {

            if (array[i] == value) {
                return i;
            }
        }

        return -1;
    }
}