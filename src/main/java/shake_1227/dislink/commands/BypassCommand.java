package shake_1227.dislink.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import shake_1227.dislink.DisLink;

import java.util.UUID;

public class BypassCommand implements CommandExecutor {

    private final DisLink plugin;

    public BypassCommand(DisLink plugin) {
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
        if (args.length < 2) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.YELLOW + "使用方法: /bypass <add/remove> <player>");
            return true;
        }

        String action = args[0].toLowerCase(); // "add" または "remove"
        String playerName = args[1];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);

        // プレイヤーが存在しない場合のエラーメッセージ
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "指定したプレイヤーが存在しません。");
            return true;
        }

        UUID uuid = targetPlayer.getUniqueId();

        // ユーザーのアクションを処理
        if (action.equals("add")) {
            // 認証不要ユーザーに追加
            if (plugin.isBypassed(uuid)) { // user.yml を参照して確認
                sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "プレイヤー " + playerName + " は既に認証不要リストに存在します。");
            } else {
                plugin.addBypassedPlayer(uuid); // user.yml に保存
                sender.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "プレイヤー " + playerName + " を認証不要リストに追加しました！");
                plugin.getLogger().info("[BypassCommand] プレイヤー " + playerName + " を認証不要リストに追加しました (UUID: " + uuid + ")");
            }
        } else if (action.equals("remove")) {
            // 認証不要ユーザーから削除
            if (plugin.isBypassed(uuid)) { // user.yml を参照して確認
                plugin.removeBypassedPlayer(uuid); // user.yml から削除
                sender.sendMessage(DisLink.PREFIX + ChatColor.GREEN + "プレイヤー " + playerName + " を認証不要リストから削除しました！");
                plugin.getLogger().info("[BypassCommand] プレイヤー " + playerName + " を認証不要リストから削除しました (UUID: " + uuid + ")");
            } else {
                sender.sendMessage(DisLink.PREFIX + ChatColor.RED + "プレイヤー " + playerName + " は認証不要リストに存在しません。");
            }
        } else {
            sender.sendMessage(DisLink.PREFIX + ChatColor.YELLOW + "使用方法: /bypass <add/remove> <player>");
        }

        return true;
    }
}
