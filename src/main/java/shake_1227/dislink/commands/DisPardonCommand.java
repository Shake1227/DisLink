package shake_1227.dislink.commands;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import shake_1227.dislink.DisLink;

import java.util.UUID;

public class DisPardonCommand implements CommandExecutor {

    private final DisLink plugin;

    public DisPardonCommand(DisLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 権限チェック
        if (!sender.hasPermission("dislink.admin")) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "このコマンドを使用する権限がありません！");
            return true;
        }

        // 引数チェック
        if (args.length < 1) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.YELLOW + "使用方法: /dispardon <player>");
            return true;
        }

        String playerName = args[0];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);

        if (!targetPlayer.hasPlayedBefore()) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "指定したプレイヤーは存在しません。");
            return true;
        }

        UUID uuid = targetPlayer.getUniqueId();

        // BANされているか確認
        if (plugin.isBanned(uuid)) {
            String discordId = plugin.getBannedDiscordId(uuid);

            // Discord ID の確認と通知
            if (discordId != null) {
                sender.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "プレイヤー " + playerName + " に紐づけられていたDiscord ID: " + discordId);
            } else {
                sender.sendMessage(DisLink.PREFIX + ChatColor.YELLOW + "プレイヤー " + playerName + " に紐づけられていたDiscord IDは見つかりませんでした。");
            }

            // Minecraft サーバーのBANを解除
            Bukkit.getBanList(BanList.Type.NAME).pardon(targetPlayer.getName());

            // banned.yml から削除
            plugin.removeFromBanList(uuid);

            // user.yml に追加
            if (discordId != null) {
                plugin.saveAuthenticatedUser(uuid, discordId);
            }

            // コマンド実行者への通知
            sender.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "プレイヤー " + playerName + " のBANを解除しました。");
            plugin.getLogger().info("[DisPardon] プレイヤー " + playerName + " のBANを解除しました。");
        } else {
            sender.sendMessage(DisLink.PREFIX + ChatColor.YELLOW + "プレイヤー " + playerName + " はBANされていません。");
        }

        return true;
    }
}
