package shake_1227.dislink;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordListener extends ListenerAdapter {

    private final DisLink plugin;

    public DiscordListener(DisLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // ボットのメッセージを無視
        if (event.getAuthor().isBot()) return;

        // 認証用チャンネルIDの確認
        String channelId = plugin.getPluginConfig().getString("discord.auth-channel-id");
        if (channelId == null || !event.getChannel().getId().equals(channelId)) {
            plugin.getLogger().info("Message received in unauthorized channel: " + event.getChannel().getId());
            return;
        }

        // Discordで送信されたメッセージを取得し、空白をトリム
        String message = event.getMessage().getContentRaw().trim();
        plugin.getLogger().info("Received message from Discord: '" + message + "'");

        // 空のメッセージや空白のみのメッセージを無視
        if (message.isEmpty()) {
            plugin.getLogger().warning("Empty or whitespace-only message received. Ignoring.");
            event.getChannel().sendMessage("認証コードを入力してください！").queue();
            return;
        }

        // 再認証処理（"再認証"と入力された場合）
        if (message.equalsIgnoreCase("再認証")) {
            handleReauth(event);
            return;
        }

        // 認証コードを照合
        UUID matchedUUID = null;
        synchronized (plugin.getPendingCodes()) { // スレッドセーフに処理
            for (Map.Entry<UUID, String> entry : plugin.getPendingCodes().entrySet()) {
                UUID uuid = entry.getKey();
                String storedCode = entry.getValue().trim(); // 念のためトリム

                // デバッグログ: 現在の認証コード
                plugin.getLogger().info("Checking code: '" + storedCode + "' for UUID: " + uuid);

                if (storedCode.equalsIgnoreCase(message)) {
                    matchedUUID = uuid;
                    break;
                }
            }
        }

        // 認証コードが一致する場合
        if (matchedUUID != null) {
            authenticateUser(matchedUUID, event);
        } else {
            // 認証コードが一致しない場合
            handleAuthFailure(event, message);
        }
    }

    private void authenticateUser(UUID matchedUUID, MessageReceivedEvent event) {
        // オフラインプレイヤーを取得
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(matchedUUID);

        if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
            // 認証成功処理
            plugin.getAuthenticatedPlayers().put(matchedUUID, true); // 認証済みユーザーとして登録
            synchronized (plugin.getPendingCodes()) { // スレッドセーフに削除
                plugin.getPendingCodes().remove(matchedUUID);
            }
            plugin.getDiscordToMinecraftMap().put(event.getAuthor().getId(), matchedUUID); // Discordアカウントと紐付け

            // Discordチャンネルに成功メッセージを送信
            String successMessage = plugin.getPluginConfig().getString("messages.auth-success", "&a認証に成功しました！プレイヤー {player} を許可しました！");
            successMessage = ChatColor.translateAlternateColorCodes('&', successMessage);
            successMessage = successMessage.replace("{player}", offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown");
            event.getChannel().sendMessage(successMessage).queue();

            // デバッグログ
            plugin.getLogger().info("Player " + offlinePlayer.getName() + " authenticated successfully with UUID: " + matchedUUID);
        } else {
            // UUIDが無効な場合のエラーメッセージ
            plugin.getLogger().warning("No valid player found for UUID: " + matchedUUID);
            event.getChannel().sendMessage("このUUIDに対応するプレイヤーが存在しません。").queue();
        }
    }

    private void handleAuthFailure(MessageReceivedEvent event, String message) {
        String failureMessage = plugin.getPluginConfig().getString("messages.auth-failure", "&c認証コードが間違っています！");
        plugin.getLogger().info("No matching code found for message: '" + message + "'");
        failureMessage = ChatColor.translateAlternateColorCodes('&', failureMessage);
        event.getChannel().sendMessage(failureMessage.replace("{code}", message)).queue();

        // プレイヤーをキック
        plugin.getLogger().info("Authentication failed. Player was not authenticated.");
    }

    private void handleReauth(MessageReceivedEvent event) {
        String discordId = event.getAuthor().getId();
        plugin.getLogger().info("Reauth request received from Discord ID: " + discordId);

        // Discordアカウントに紐付けされたプレイヤーを確認
        if (!plugin.getDiscordToMinecraftMap().containsKey(discordId)) {
            String reauthFailureMessage = plugin.getPluginConfig().getString("messages.reauth-failure", "&c再認証に失敗しました。アカウントが見つかりません。");
            reauthFailureMessage = ChatColor.translateAlternateColorCodes('&', reauthFailureMessage);
            plugin.getLogger().info("No player found for Discord ID: " + discordId);
            event.getChannel().sendMessage(reauthFailureMessage).queue();
            return;
        }

        // 紐付け解除処理
        UUID uuid = plugin.getDiscordToMinecraftMap().remove(discordId);
        plugin.getAuthenticatedPlayers().remove(uuid); // 認証済みリストから削除

        // Discordチャンネルに成功メッセージを送信
        String reauthSuccessMessage = plugin.getPluginConfig().getString("messages.reauth-success", "&a再認証が完了しました！");
        reauthSuccessMessage = ChatColor.translateAlternateColorCodes('&', reauthSuccessMessage);
        event.getChannel().sendMessage(reauthSuccessMessage).queue();

        // デバッグログ
        plugin.getLogger().info("Reauthentication completed. UUID " + uuid + " has been unlinked.");
    }
}
