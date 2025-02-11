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

public class DisBanCommand implements CommandExecutor {
    private final DisLink plugin;
    public DisBanCommand(DisLink plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dislink.admin")) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "このコマンドを使用する権限がありません！");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.YELLOW + "使用方法: /disban <player> [reason]");
            return true;
        }
        String playerName = args[0];
        String reason = args.length > 1 ? String.join(" ", args).substring(playerName.length()).trim() : "理由なし";
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
        if (!targetPlayer.hasPlayedBefore()) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "指定したプレイヤーは存在しません。");
            return true;
        }
        UUID uuid = targetPlayer.getUniqueId();
        String discordId = plugin.getAuthenticatedDiscordId(uuid);
        if (discordId == null || discordId.isEmpty()) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.YELLOW + "プレイヤー " + playerName + " に紐づけられたDiscordアカウントが見つかりません。");
            plugin.getLogger().warning("[DisBan] Discord IDが見つかりません: UUID=" + uuid + ", Player=" + playerName);
        } else {
            sender.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "プレイヤー " + playerName + " に紐づけられていたDiscord ID: " + discordId);
            plugin.getLogger().info("[DisBan] Discord IDを取得しました: " + discordId);
        }
        if (plugin.isAuthenticated(uuid)) {
            plugin.removeAuthenticatedUser(uuid);
            plugin.getLogger().info("[DisBan] " + playerName + " (UUID: " + uuid + ") をuser.ymlから削除しました。");
        }
        Bukkit.getBanList(BanList.Type.NAME).addBan(targetPlayer.getName(), reason, null, sender.getName());
        plugin.addToBanList(uuid, discordId, reason);
        if (targetPlayer.isOnline()) {
            targetPlayer.getPlayer().kickPlayer(DisLink.PREFIX + ChatColor.RED + "あなたはBANされました。\n理由: " + reason);
        }
        sender.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "プレイヤー " + playerName + " をBANしました。\n理由: " + reason);
        plugin.getLogger().info("[DisBan] プレイヤー " + playerName + " をBANしました。理由: " + reason);
        return true;
    }
}