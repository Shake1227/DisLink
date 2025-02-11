package shake_1227.dislink.gui;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import shake_1227.dislink.DisLink;

public class GUIEventHandler implements Listener {
    private final DisLink plugin;
    public GUIEventHandler(DisLink plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inventory = event.getInventory();
        String title = event.getView().getTitle();
        if (!isDisLinkGUI(title)) return;
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        if (title.equals(ChatColor.LIGHT_PURPLE + "DisLink メニュー")) {
            DisLinkGUI.handleGUIClick(event);
        } else if (title.contains("認証不要ユーザー管理")) {
            DisLinkGUI.handleBypassPlayerClick(event, player);
        } else if (title.contains("認証済みユーザー管理")) {
            DisLinkGUI.handleReauthPlayerClick(event, player);
        }
    }
    private boolean isDisLinkGUI(String title) {
        String strippedTitle = ChatColor.stripColor(title);
        return strippedTitle.equals("DisLink メニュー")
                || strippedTitle.contains("認証不要ユーザー管理")
                || strippedTitle.contains("認証済みユーザー管理");
    }
}
