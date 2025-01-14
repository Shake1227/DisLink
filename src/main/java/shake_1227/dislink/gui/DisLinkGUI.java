package shake_1227.dislink.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import shake_1227.dislink.DisLink;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DisLinkGUI {

    private static DisLink plugin;

    public static void initialize(DisLink pl) {
        plugin = pl;
    }

    // メインGUI
    public static Inventory getMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.LIGHT_PURPLE + "DisLink メニュー");

        // 認証不要プレイヤー管理
        gui.setItem(11, createItem(Material.PLAYER_HEAD, ChatColor.YELLOW + "認証不要プレイヤーを管理", "プレイヤーのリストを表示します。"));

        // 認証解除
        gui.setItem(13, createItem(Material.BARRIER, ChatColor.RED + "認証解除", "認証解除するプレイヤーのリストを表示します。"));

        // 認証不要リスト
        gui.setItem(15, createItem(Material.BOOK, ChatColor.AQUA + "認証不要リスト", "現在の認証不要リストを表示します。"));

        return gui;
    }

    // サブGUI: 認証不要プレイヤー追加用
    public static Inventory getBypassPlayerGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.YELLOW + "認証不要プレイヤーを追加");

        int slot = 0;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            gui.setItem(slot, createPlayerHead(onlinePlayer, ChatColor.GREEN + onlinePlayer.getName()));
            slot++;
        }

        return gui;
    }

    // サブGUI: 認証解除用
    public static Inventory getReauthPlayerGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.RED + "認証解除するプレイヤー");

        int slot = 0;
        for (UUID uuid : plugin.getPendingCodes().keySet()) {
            Player pendingPlayer = Bukkit.getPlayer(uuid);
            if (pendingPlayer != null) {
                gui.setItem(slot, createPlayerHead(pendingPlayer, ChatColor.YELLOW + pendingPlayer.getName()));
                slot++;
            }
        }

        return gui;
    }

    // アイテム作成用
    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(loreList);

        item.setItemMeta(meta);
        return item;
    }

    // プレイヤーヘッド作成用
    private static ItemStack createPlayerHead(Player player, String displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        meta.setDisplayName(displayName);
        head.setItemMeta(meta);
        return head;
    }

    // GUIクリックイベント
    public static void handleGUIClick(InventoryClickEvent event, Player player) {
        if (event.getCurrentItem() == null || !event.getView().getTitle().contains("DisLinker")) return;

        event.setCancelled(true);
        String title = event.getView().getTitle();

        if (title.contains("認証不要プレイヤーを管理")) {
            player.openInventory(getBypassPlayerGUI(player));
        } else if (title.contains("認証解除するプレイヤー")) {
            player.openInventory(getReauthPlayerGUI(player));
        }
    }
}
