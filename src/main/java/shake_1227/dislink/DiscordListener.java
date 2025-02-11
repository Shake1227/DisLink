package shake_1227.dislink;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import java.util.Map;
import java.util.UUID;

public class DiscordListener extends ListenerAdapter {
    private final DisLink plugin;
    public DiscordListener(DisLink plugin) {
        this.plugin = plugin;
    }
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        String channelId = plugin.getPluginConfig().getString("discord.auth-channel-id");
        if (channelId == null || !event.getChannel().getId().equals(channelId)) {
            plugin.getLogger().info("Message received in unauthorized channel: " + event.getChannel().getId());
            return;
        }
        String message = event.getMessage().getContentRaw().trim();
        plugin.getLogger().info("Received message from Discord: '" + message + "'");
        if (message.isEmpty()) {
            plugin.getLogger().warning("Empty or whitespace-only message received. Ignoring.");
            event.getChannel().sendMessage("認証コードを入力してください！").queue();
            return;
        }
        if (message.equalsIgnoreCase("再認証")) {
            handleReauth(event);
            return;
        }
        UUID matchedUUID = null;
        synchronized (plugin.getPendingCodes()) {
            for (Map.Entry<UUID, String> entry : plugin.getPendingCodes().entrySet()) {
                UUID uuid = entry.getKey();
                String storedCode = entry.getValue().trim();
                plugin.getLogger().info("Checking code: '" + storedCode + "' for UUID: " + uuid);
                if (storedCode.equalsIgnoreCase(message)) {
                    matchedUUID = uuid;
                    break;
                }
            }
        }
        if (matchedUUID != null) {
            authenticateUser(matchedUUID, event);
        } else {
            handleAuthFailure(event, message);
        }
    }
    private void authenticateUser(UUID matchedUUID, MessageReceivedEvent event) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(matchedUUID);
        String discordId = event.getAuthor().getId();
        for (String uuidString : plugin.getUsersConfig().getConfigurationSection("authenticated").getKeys(false)) {
            String existingDiscordId = plugin.getUsersConfig().getString("authenticated." + uuidString);
            if (existingDiscordId != null && existingDiscordId.equals(discordId) && !uuidString.equals(matchedUUID.toString())) {
                String errorMessage = plugin.getPluginConfig().getString(
                        "messages.discord-id-already-linked",
                        "&cこのDiscordアカウントはすでに別のMinecraftアカウントと紐付けられています！"
                );
                errorMessage = ChatColor.translateAlternateColorCodes('&', errorMessage);
                event.getChannel().sendMessage(errorMessage).queue();
                plugin.getLogger().warning("Discord ID " + discordId + " is already linked to UUID " + uuidString);
                return;
            }
        }
        if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
            plugin.getAuthenticatedPlayers().put(matchedUUID, discordId);
            synchronized (plugin.getPendingCodes()) {
                plugin.getPendingCodes().remove(matchedUUID);
            }
            plugin.getDiscordToMinecraftMap().put(discordId, matchedUUID);
            plugin.saveAuthenticatedUser(matchedUUID, discordId);
            String successMessage = plugin.getPluginConfig().getString(
                    "messages.auth-success",
                    "&a認証に成功しました！プレイヤー {player} を許可しました！"
            );
            successMessage = ChatColor.translateAlternateColorCodes('&', successMessage);
            successMessage = successMessage.replace("{player}", offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown");
            event.getChannel().sendMessage(successMessage).queue();
            plugin.getLogger().info("Player " + offlinePlayer.getName() + " authenticated successfully with UUID: " + matchedUUID);
        } else {
            plugin.getLogger().warning("No valid player found for UUID: " + matchedUUID);
            event.getChannel().sendMessage("このUUIDに対応するプレイヤーが存在しません。").queue();
        }
    }
    private void handleAuthFailure(MessageReceivedEvent event, String message) {
        String failureMessage = plugin.getPluginConfig().getString("messages.auth-failure", "&c認証コードが間違っています！");
        plugin.getLogger().info("No matching code found for message: '" + message + "'");
        failureMessage = ChatColor.translateAlternateColorCodes('&', failureMessage);
        event.getChannel().sendMessage(failureMessage.replace("{code}", message)).queue();
        plugin.getLogger().info("Authentication failed. Player was not authenticated.");
    }
    private void handleReauth(MessageReceivedEvent event) {
        String discordId = event.getAuthor().getId();
        plugin.getLogger().info("Reauth request received from Discord ID: " + discordId);
        if (!plugin.getDiscordToMinecraftMap().containsKey(discordId)) {
            String reauthFailureMessage = plugin.getPluginConfig().getString("messages.reauth-failure", "&c再認証に失敗しました。アカウントが見つかりません。");
            reauthFailureMessage = ChatColor.translateAlternateColorCodes('&', reauthFailureMessage);
            plugin.getLogger().info("No player found for Discord ID: " + discordId);
            event.getChannel().sendMessage(reauthFailureMessage).queue();
            return;
        }
        UUID uuid = plugin.getDiscordToMinecraftMap().remove(discordId);
        plugin.getAuthenticatedPlayers().remove(uuid);
        plugin.removeAuthenticatedUser(uuid);
        String reauthSuccessMessage = plugin.getPluginConfig().getString("messages.reauth-success", "&a再認証が完了しました！");
        reauthSuccessMessage = ChatColor.translateAlternateColorCodes('&', reauthSuccessMessage);
        event.getChannel().sendMessage(reauthSuccessMessage).queue();
        plugin.getLogger().info("Reauthentication completed. UUID " + uuid + " has been unlinked.");
    }
}
