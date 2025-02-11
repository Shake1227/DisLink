package shake_1227.dislink.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
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
    public static Inventory getMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.LIGHT_PURPLE + "DisLink メニュー");
        gui.setItem(11, createItem(Material.PLAYER_HEAD, ChatColor.YELLOW + "認証不要ユーザー管理", "クリックして認証不要ユーザーを管理"));
        gui.setItem(13, createItem(Material.BARRIER, ChatColor.RED + "認証済みプレイヤー管理", "クリックして認証済みユーザーを管理"));
        gui.setItem(15, createItem(Material.BOOK, ChatColor.AQUA + "認証済みプレイヤー一覧", "クリックしてチャットに一覧を表示"));
        return gui;
    }
    public static Inventory getBypassPlayerGUI(Player player, int page) {
        List<OfflinePlayer> allPlayers = List.of(Bukkit.getOfflinePlayers());
        int totalPages = (int) Math.ceil(allPlayers.size() / 45.0);
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.YELLOW + "認証不要ユーザー管理: " + page + "/" + totalPages);
        int startIndex = (page - 1) * 45;
        int endIndex = Math.min(startIndex + 45, allPlayers.size());
        for (int i = startIndex; i < endIndex; i++) {
            OfflinePlayer offlinePlayer = allPlayers.get(i);
            boolean isBypassed = plugin.isBypassed(offlinePlayer.getUniqueId());
            gui.addItem(createPlayerHead(offlinePlayer, isBypassed ? ChatColor.GREEN + offlinePlayer.getName() : ChatColor.RED + offlinePlayer.getName()));
        }
        if (page > 1) gui.setItem(45, createItem(Material.ARROW, ChatColor.YELLOW + "前のページ"));
        if (page < totalPages) gui.setItem(53, createItem(Material.ARROW, ChatColor.YELLOW + "次のページ"));
        return gui;
    }
    public static Inventory getReauthPlayerGUI(Player player, int page) {
        List<UUID> authenticatedPlayers = new ArrayList<>(plugin.getAuthenticatedPlayers().keySet());
        int totalPages = (int) Math.ceil(authenticatedPlayers.size() / 45.0);
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.RED + "認証済みユーザー管理: " + page + "/" + totalPages);
        int startIndex = (page - 1) * 45;
        int endIndex = Math.min(startIndex + 45, authenticatedPlayers.size());
        for (int i = startIndex; i < endIndex; i++) {
            UUID uuid = authenticatedPlayers.get(i);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String discordId = plugin.getAuthenticatedDiscordId(uuid);
            gui.addItem(createPlayerHead(offlinePlayer, ChatColor.YELLOW + offlinePlayer.getName(), ChatColor.GRAY + "Discord ID: " + discordId));
        }
        if (page > 1) gui.setItem(45, createItem(Material.ARROW, ChatColor.YELLOW + "前のページ"));
        if (page < totalPages) gui.setItem(53, createItem(Material.ARROW, ChatColor.YELLOW + "次のページ"));

        return gui;
    }
    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
    private static ItemStack createPlayerHead(OfflinePlayer player, String displayName, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(displayName);
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(loreList);
            head.setItemMeta(meta);
        }
        return head;
    }
    public static void handleGUIClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        event.setCancelled(true);
        if (title.equals(ChatColor.LIGHT_PURPLE + "DisLink メニュー")) {
            switch (event.getCurrentItem().getType()) {
                case PLAYER_HEAD:
                    player.openInventory(getBypassPlayerGUI(player, 1));
                    break;
                case BARRIER:
                    player.openInventory(getReauthPlayerGUI(player, 1));
                    break;
                case BOOK:
                    player.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "認証済みプレイヤー一覧:");
                    for (UUID uuid : plugin.getAuthenticatedPlayers().keySet()) {
                        String name = Bukkit.getOfflinePlayer(uuid).getName();
                        String discordId = plugin.getAuthenticatedDiscordId(uuid);
                        player.sendMessage(DisLink.PREFIX + ChatColor.YELLOW + "- " + name + ChatColor.GRAY + " (Discord ID: " + discordId + ")");
                    }
                    break;
                default:
                    break;
            }
        }
        if (title.contains("認証不要ユーザー管理")) {
            handleBypassPlayerClick(event, player);
        }
        if (title.contains("認証済みユーザー管理")) {
            handleReauthPlayerClick(event, player);
        }
    }
    public static void handleBypassPlayerClick(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;
        String playerName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (plugin.isBypassed(target.getUniqueId())) {
            plugin.removeBypassedPlayer(target.getUniqueId());
            player.sendMessage(DisLink.PREFIX + ChatColor.RED + target.getName() + " を認証不要リストから削除しました。");
        } else {
            plugin.addBypassedPlayer(target.getUniqueId());
            player.sendMessage(DisLink.PREFIX + ChatColor.GREEN + target.getName() + " を認証不要リストに追加しました。");
        }
        player.openInventory(getBypassPlayerGUI(player, 1)); // 更新
    }
    public static void handleReauthPlayerClick(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;
        String playerName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        plugin.removeAuthenticatedUser(target.getUniqueId());
        player.sendMessage(DisLink.PREFIX + ChatColor.RED + target.getName() + " の認証を解除しました。");
        player.openInventory(getReauthPlayerGUI(player, 1)); // 更新
    }
}
