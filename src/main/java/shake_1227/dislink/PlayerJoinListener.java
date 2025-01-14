package shake_1227.dislink;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

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

        // プレイヤーが認証不要リストにいる場合
        if (plugin.getBypassedPlayers().getOrDefault(uuid, false)) {
            player.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "認証不要プレイヤーとしてサーバーに参加しました！");
            return;
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
