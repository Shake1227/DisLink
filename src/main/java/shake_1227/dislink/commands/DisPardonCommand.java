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
        if (!sender.hasPermission("dislink.admin")) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "このコマンドを使用する権限がありません！");
            return true;
        }
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
        if (plugin.isBanned(uuid)) {
            String discordId = plugin.getBannedDiscordId(uuid);
            if (discordId != null) {
                sender.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "プレイヤー " + playerName + " に紐づけられていたDiscord ID: " + discordId);
            } else {
                sender.sendMessage(DisLink.PREFIX + ChatColor.YELLOW + "プレイヤー " + playerName + " に紐づけられていたDiscord IDは見つかりませんでした。");
            }
            Bukkit.getBanList(BanList.Type.NAME).pardon(targetPlayer.getName());
            plugin.removeFromBanList(uuid);
            if (discordId != null) {
                plugin.saveAuthenticatedUser(uuid, discordId);
            }
            sender.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "プレイヤー " + playerName + " のBANを解除しました。");
            plugin.getLogger().info("[DisPardon] プレイヤー " + playerName + " のBANを解除しました。");
        } else {
            sender.sendMessage(DisLink.PREFIX + ChatColor.YELLOW + "プレイヤー " + playerName + " はBANされていません。");
        }
        return true;
    }
}
