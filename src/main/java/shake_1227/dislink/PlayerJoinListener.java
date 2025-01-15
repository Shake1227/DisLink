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

        // 認証不要ユーザーの場合はそのまま参加を許可
        if (plugin.getBypassedPlayers().getOrDefault(uuid, false)) {
            player.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "認証不要プレイヤーとしてサーバーに参加しました！");
            return;
        }

        // 認証済みユーザーの場合はそのまま参加を許可
        if (plugin.isAuthenticated(uuid)) {
            player.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "認証済みプレイヤーとしてサーバーに参加しました！");
            return;
        }

        // item-enableがtrueの場合、OPプレイヤーにアイテムを付与
        if (plugin.getPluginConfig().getBoolean("item-enable", false)) {
            if (player.isOp()) {
                // DisLinker アイテムを作成
                ItemStack disLinker = new ItemStack(Material.STICK);
                ItemMeta meta = disLinker.getItemMeta();

                if (meta != null) {
                    meta.setDisplayName(ChatColor.YELLOW + "DisLinker");
                    meta.setCustomModelData(1); // カスタムモデルデータを設定
                    disLinker.setItemMeta(meta);
                }

                // アイテムを付与
                player.getInventory().addItem(disLinker);
                player.sendMessage(DisLink.PREFIX + ChatColor.GOLD + "OPプレイヤーとしてDisLinkerを受け取りました！");
            }

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

        // ランダムな認証コードを生成
        String authCode = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999)); // 4桁
        plugin.getPendingCodes().put(uuid, authCode);

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
}
