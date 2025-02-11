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
        if (plugin.getPluginConfig().getBoolean("item-enable", false) && player.isOp()) {
            giveOpItem(player);
        }
        if (plugin.isBypassed(uuid)) {
            player.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "認証不要プレイヤーとしてサーバーに参加しました！");
            return;
        }
        if (plugin.isAuthenticated(uuid)) {
            String discordId = plugin.getAuthenticatedDiscordId(uuid);
            if (discordId != null && !discordId.isEmpty()) {
                player.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "認証済みプレイヤーとしてサーバーに参加しました！");
                return;
            } else {
                plugin.removeAuthenticatedUser(uuid);
                player.kickPlayer(ChatColor.RED + "認証データが無効です。再度認証してください。");
                return;
            }
        }
        handleUnauthenticatedPlayer(player, uuid);
    }
    private void handleUnauthenticatedPlayer(Player player, UUID uuid) {
        String authCode = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999)); // 4桁
        plugin.getPendingCodes().put(uuid, authCode);
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
    private void giveOpItem(Player player) {
        ItemStack disLinker = new ItemStack(Material.STICK);
        ItemMeta meta = disLinker.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "DisLinker");
            meta.setCustomModelData(1);
            disLinker.setItemMeta(meta);
        }
        player.getInventory().addItem(disLinker);
        player.sendMessage(DisLink.PREFIX + ChatColor.GOLD + "OPプレイヤーとしてDisLinkerを受け取りました！");
        String resourcePackUrl = plugin.getPluginConfig().getString("resourcepack-url");
        if (resourcePackUrl != null && !resourcePackUrl.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setResourcePack(resourcePackUrl);
                    player.sendMessage(DisLink.PREFIX + ChatColor.AQUA + "リソースパックが適用されました！");
                }
            }, 100L);
        } else {
            plugin.getLogger().warning(DisLink.PREFIX + "リソースパックURLが設定されていません！ config.yml を確認してください。");
        }
    }
}
