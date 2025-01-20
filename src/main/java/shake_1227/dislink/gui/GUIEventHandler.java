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
        // クリックしたプレイヤーを取得
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // 開いているインベントリとタイトルを取得
        Inventory inventory = event.getInventory();
        String title = event.getView().getTitle();

        // プラグインが作成したGUIかどうかを判定
        if (!isDisLinkGUI(title)) return; // プラグインのGUIでなければスキップ

        // プラグインのGUIであればクリックイベントをキャンセル（アイテム移動を防ぐ）
        event.setCancelled(true);

        // クリックしたアイテムを取得
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        // GUIタイトルに基づいて処理を分岐
        if (title.equals(ChatColor.LIGHT_PURPLE + "DisLink メニュー")) {
            DisLinkGUI.handleGUIClick(event); // メインメニューの処理を委譲
        } else if (title.contains("認証不要ユーザー管理")) {
            DisLinkGUI.handleBypassPlayerClick(event, player); // 認証不要プレイヤー管理の処理
        } else if (title.contains("認証済みユーザー管理")) {
            DisLinkGUI.handleReauthPlayerClick(event, player); // 認証済みユーザー管理の処理
        }
    }

    /**
     * プラグインが作成したGUIかどうかを判定する
     *
     * @param title インベントリタイトル
     * @return プラグインのGUIならtrue、そうでなければfalse
     */
    private boolean isDisLinkGUI(String title) {
        String strippedTitle = ChatColor.stripColor(title); // タイトルから色コードを除去
        return strippedTitle.equals("DisLink メニュー")
                || strippedTitle.contains("認証不要ユーザー管理")
                || strippedTitle.contains("認証済みユーザー管理");
    }
}
