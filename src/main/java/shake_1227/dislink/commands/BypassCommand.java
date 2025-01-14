package shake_1227.dislink.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import shake_1227.dislink.DisLink;

import java.util.UUID;

public class BypassCommand implements CommandExecutor {

    private final DisLink plugin;

    public BypassCommand(DisLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dislink.admin")) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "このコマンドを使用する権限がありません！");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.YELLOW + "使用方法: /bypass <add/remove> <player>");
            return true;
        }

        String action = args[0].toLowerCase();
        String playerName = args[1];
        Player targetPlayer = Bukkit.getPlayer(playerName);

        if (action.equals("add")) {
            if (targetPlayer == null) {
                sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "指定したプレイヤーがオンラインではありません。");
                return true;
            }

            UUID uuid = targetPlayer.getUniqueId();
            plugin.getBypassedPlayers().put(uuid, true);
            sender.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "プレイヤー " + playerName + " を認証不要リストに追加しました！");
        } else if (action.equals("remove")) {
            if (targetPlayer == null) {
                sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "指定したプレイヤーがオンラインではありません。");
                return true;
            }

            UUID uuid = targetPlayer.getUniqueId();
            if (plugin.getBypassedPlayers().remove(uuid) != null) {
                sender.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "プレイヤー " + playerName + " を認証不要リストから削除しました！");
            } else {
                sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "プレイヤー " + playerName + " は認証不要リストに存在しません。");
            }
        } else {
            sender.sendMessage(DisLink.PREFIX + ChatColor.YELLOW + "使用方法: /bypass <add/remove> <player>");
        }

        return true;
    }
}