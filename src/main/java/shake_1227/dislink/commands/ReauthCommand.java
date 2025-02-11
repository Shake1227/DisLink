package shake_1227.dislink.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import shake_1227.dislink.DisLink;
import java.util.UUID;

public class ReauthCommand implements CommandExecutor {

    private final DisLink plugin;
    public ReauthCommand(DisLink plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dislink.admin")) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "このコマンドを使用する権限がありません！");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.YELLOW + "使用方法: /reauth <player>");
            return true;
        }
        String playerName = args[0];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "指定したプレイヤーは存在しません。");
            return true;
        }
        UUID uuid = targetPlayer.getUniqueId();
        if (plugin.isAuthenticated(uuid)) {
            plugin.removeAuthenticatedUser(uuid);
            if (targetPlayer.isOnline()) {
                Player onlinePlayer = targetPlayer.getPlayer();
                if (onlinePlayer != null) {
                    onlinePlayer.kickPlayer(ChatColor.RED + "再認証が要求されました。");
                }
            }
            sender.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "プレイヤー " + playerName + " の認証を解除しました。");
            plugin.getLogger().info(DisLink.PREFIX + "認証解除: " + playerName + " (UUID: " + uuid + ")");
        } else {
            sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "プレイヤー " + playerName + " は認証済みユーザーではありません。");
        }
        return true;
    }
}
