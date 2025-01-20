package shake_1227.dislink;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerJoinListener implements Listener {

    private final DisLink plugin;

    public PlayerJoinListener(DisLink plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // item-enableがtrueの場合、OPプレイヤーにアイテムを付与
        if (plugin.getPluginConfig().getBoolean("item-enable", false) && player.isOp()) {
            giveOpItem(player); // OPプレイヤー用アイテム付与メソッドを呼び出し
        }

        // 認証不要ユーザーかどうかを user.yml から参照
        if (plugin.isBypassed(uuid)) {
            player.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "認証不要プレイヤーとしてサーバーに参加しました！");
            return; // 認証不要ユーザーはそのまま許可
        }

        // 認証済みユーザーかどうかを user.yml から参照し、UUID と Discord ID が正しく保存されているかを確認
        if (plugin.isAuthenticated(uuid)) {
            // users.yml に正しい Discord ID が存在しているか確認
            String discordId = plugin.getAuthenticatedDiscordId(uuid);
            if (discordId != null && !discordId.isEmpty()) {
                player.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "認証済みプレイヤーとしてサーバーに参加しました！");
                return; // 認証済みユーザーはそのまま許可
            } else {
                // 認証済みリストに不正データがあれば削除し、再認証を求める
                plugin.removeAuthenticatedUser(uuid);
                player.kickPlayer(ChatColor.RED + "認証データが無効です。再度認証してください。");
                return;
            }
        }

        // 認証が必要なユーザーの処理
        handleUnauthenticatedPlayer(player, uuid);
    }

    /**
     * 認証が必要なプレイヤーを処理する
     *
     * @param player プレイヤー
     * @param uuid   プレイヤーのUUID
     */
    private void handleUnauthenticatedPlayer(Player player, UUID uuid) {
        // ランダムな認証コードを生成
        String authCode = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999)); // 4桁
        plugin.getPendingCodes().put(uuid, authCode); // 認証コードを保留リストに追加

        // プレイヤーをキックして認証コードを表示
        String kickMessage = plugin.getPluginConfig().getString("messages.kick-message");
        if (kickMessage != null) {
            kickMessage = kickMessage.replace("{code}", authCode);
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMessage));
        } else {
            player.kickPlayer(ChatColor.RED + "サーバーに参加するには認証が必要です。\n"
                    + ChatColor.YELLOW + "Discord チャンネルに以下の認証コードを送信してください:\n"
                    + ChatColor.AQUA + authCode);
        }
    }

    /**
     * OPプレイヤーにカスタムアイテムを付与する
     *
     * @param player OP権限を持つプレイヤー
     */
    private void giveOpItem(Player player) {
        // DisLinker アイテムを作成
        ItemStack disLinker = new ItemStack(Material.STICK); // 任意のMaterialを選択
        ItemMeta meta = disLinker.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "DisLinker");
            meta.setCustomModelData(1); // カスタムモデルデータを設定
            disLinker.setItemMeta(meta);
        }

        // アイテムを付与
        player.getInventory().addItem(disLinker);
        player.sendMessage(DisLink.PREFIX + ChatColor.GOLD + "OPプレイヤーとしてDisLinkerを受け取りました！");

        // リソースパックの適用
        String resourcePackUrl = plugin.getPluginConfig().getString("resourcepack-url");
        if (resourcePackUrl != null && !resourcePackUrl.isEmpty()) {
            // 5秒後にリソースパックを適用
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setResourcePack(resourcePackUrl);
                    player.sendMessage(DisLink.PREFIX + ChatColor.AQUA + "リソースパックが適用されました！");
                }
            }, 100L); // 100L = 5秒 (1秒 = 20 ticks)
        } else {
            plugin.getLogger().warning(DisLink.PREFIX + "リソースパックURLが設定されていません！ config.yml を確認してください。");
        }
    }
}
