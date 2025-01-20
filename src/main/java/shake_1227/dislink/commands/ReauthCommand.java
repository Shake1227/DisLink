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
        // 権限チェック
        if (!sender.hasPermission("dislink.admin")) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "このコマンドを使用する権限がありません！");
            return true;
        }

        // コマンドの引数チェック
        if (args.length < 1) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.YELLOW + "使用方法: /reauth <player>");
            return true;
        }

        // 引数からプレイヤー名を取得
        String playerName = args[0];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName); // オフラインプレイヤーも取得可能

        // プレイヤーが存在するか確認
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "指定したプレイヤーは存在しません。");
            return true;
        }

        // UUIDを取得
        UUID uuid = targetPlayer.getUniqueId();

        // 認証済みユーザーリストに存在するか確認
        if (plugin.isAuthenticated(uuid)) {
            // 認証済みユーザーから削除
            plugin.removeAuthenticatedUser(uuid);

            // プレイヤーがオンラインの場合はキック
            if (targetPlayer.isOnline()) {
                Player onlinePlayer = targetPlayer.getPlayer();
                if (onlinePlayer != null) {
                    // 赤文字でキックメッセージを表示
                    onlinePlayer.kickPlayer(ChatColor.RED + "再認証が要求されました。");
                }
            }

            // コマンド送信者に成功メッセージを表示
            sender.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "プレイヤー " + playerName + " の認証を解除しました。");
            plugin.getLogger().info(DisLink.PREFIX + "認証解除: " + playerName + " (UUID: " + uuid + ")");
        } else {
            // プレイヤーが認証済みリストに存在しない場合
            sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "プレイヤー " + playerName + " は認証済みユーザーではありません。");
        }

        return true;
    }
}
