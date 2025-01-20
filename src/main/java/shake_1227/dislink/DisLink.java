package shake_1227.dislink;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import shake_1227.dislink.commands.BypassCommand;
import shake_1227.dislink.commands.DisBanCommand;
import shake_1227.dislink.commands.DisPardonCommand;
import shake_1227.dislink.commands.ReauthCommand;
import shake_1227.dislink.gui.DisLinkGUI;
import shake_1227.dislink.gui.DisLinkStickHandler;
import shake_1227.dislink.gui.GUIEventHandler;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class DisLink extends JavaPlugin {

    private JDA jda;
    private FileConfiguration config;
    private File usersFile;
    private FileConfiguration usersConfig;

    private File bannedFile;
    private FileConfiguration bannedConfig;

    // 認証コードの保留リスト (認証待ち)
    private final Map<UUID, String> pendingCodes = new HashMap<>();

    // 認証不要ユーザーのリスト (認証が不要なプレイヤー)
    private final Map<UUID, Boolean> bypassedPlayers = new HashMap<>();

    // 認証済みユーザーのリスト (認証を完了したプレイヤー)
    private final Map<UUID, String> authenticatedPlayers = new HashMap<>(); // Discord ID を保持

    // DiscordアカウントとMinecraft UUIDのマッピング
    private final Map<String, UUID> discordToMinecraftMap = new HashMap<>();

    public static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.LIGHT_PURPLE + "Dis" + ChatColor.YELLOW + "Link" + ChatColor.GRAY + "] ";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = getConfig();

        // ユーザーデータをロード
        loadUserData();

        // BANデータをロード
        loadBanData();

        // リスナーを登録
        getServer().getPluginManager().registerEvents(new DisLinkStickHandler(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIEventHandler(this), this);
        DisLinkGUI.initialize(this);


        // コマンドを登録
        registerCommand("disban", new DisBanCommand(this));
        registerCommand("dispardon", new DisPardonCommand(this));
        registerCommand("bypass", new BypassCommand(this));
        registerCommand("reauth", new ReauthCommand(this));

        // GUIの初期化
        DisLinkGUI.initialize(this);

        // Discord Bot の初期化
        try {
            initializeDiscordBot();
        } catch (LoginException e) {
            getLogger().severe(PREFIX + "Discord Bot の初期化に失敗しました。");
        }

        getLogger().info(PREFIX + "プラグインが有効化されました！");
    }

    @Override
    public void onDisable() {
        if (jda != null) {
            jda.shutdown();
        }
        saveUserData();
        getLogger().info(PREFIX + "プラグインが無効化されました！");
    }


    private void initializeDiscordBot() throws LoginException {
        String botToken = config.getString("discord.bot-token");
        if (botToken == null || botToken.isEmpty()) {
            throw new IllegalArgumentException(PREFIX + "Discord Bot トークンが設定されていません！ config.yml を確認してください！");
        }

        jda = JDABuilder.createDefault(botToken)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new DiscordListener(this))
                .build();

        getLogger().info(PREFIX + "Discord Bot が正常に初期化されました！");
    }

    private void registerCommand(String name, Object executor) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor((org.bukkit.command.CommandExecutor) executor);
        } else {
            getLogger().severe(PREFIX + "コマンド '/" + name + "' の登録に失敗しました！ plugin.yml を確認してください。");
        }
    }

    private void synchronizeBanList() {
        if (bannedConfig.contains("banned")) {
            for (String uuidString : bannedConfig.getConfigurationSection("banned").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                String playerName = Bukkit.getOfflinePlayer(uuid).getName();
                String reason = bannedConfig.getString("banned." + uuidString + ".reason", "指定なし");

                // サーバーのBANリストに追加
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(playerName, reason, null, "DisLink Plugin");
            }
        }
    }


    private void loadBanData() {
        bannedFile = new File(getDataFolder(), "banned.yml");
        if (!bannedFile.exists()) {
            try {
                if (bannedFile.createNewFile()) {
                    getLogger().info(PREFIX + "banned.yml ファイルを新規作成しました。");
                }
            } catch (IOException e) {
                getLogger().severe(PREFIX + "banned.yml ファイルの作成中にエラーが発生しました！");
                e.printStackTrace();
            }
        }
        bannedConfig = YamlConfiguration.loadConfiguration(bannedFile);
    }

    public void saveBanData() {
        try {
            bannedConfig.save(bannedFile);
            getLogger().info(PREFIX + "BANデータを保存しました。");
        } catch (IOException e) {
            getLogger().severe(PREFIX + "BANデータの保存中にエラーが発生しました！");
            e.printStackTrace();
        }
    }

    public void addToBanList(UUID uuid, String discordId, String reason) {
        bannedConfig.set("banned." + uuid.toString() + ".discordId", discordId);
        bannedConfig.set("banned." + uuid.toString() + ".reason", reason);
        saveBanData();
    }

    public void removeFromBanList(UUID uuid) {
        bannedConfig.set("banned." + uuid.toString(), null);
        saveBanData();
    }

    public boolean isBanned(UUID uuid) {
        return bannedConfig.contains("banned." + uuid.toString());
    }

    public String getBannedDiscordId(UUID uuid) {
        return bannedConfig.getString("banned." + uuid.toString() + ".discordId");
    }

    public String getBanReason(UUID uuid) {
        return bannedConfig.getString("banned." + uuid.toString() + ".reason");
    }

    public void saveAuthenticatedUser(UUID uuid, String discordId) {
        // 1. authenticatedPlayers マップに追加
        authenticatedPlayers.put(uuid, discordId);

        // 2. usersConfig に保存
        usersConfig.set("authenticated." + uuid.toString(), discordId);

        // 3. 保存処理のログを出力
        getLogger().info(PREFIX + "認証済みユーザーを保存しました: UUID=" + uuid + ", DiscordID=" + discordId);

        // 4. データを保存
        saveUserData();
    }



    public void removeAuthenticatedUser(UUID uuid) {
        authenticatedPlayers.remove(uuid);
        usersConfig.set("authenticated." + uuid.toString(), null); // 削除
        saveUserData();
    }

    public boolean isBypassed(UUID uuid) {
        // users.yml の "bypassed" セクションを確認
        List<String> bypassedList = usersConfig.getStringList("bypassed");
        return bypassedList.contains(uuid.toString());
    }

    public void displayAuthenticatedPlayers(Player player) {
        player.sendMessage(ChatColor.AQUA + "認証済みプレイヤー一覧:");
        for (Map.Entry<UUID, String> entry : authenticatedPlayers.entrySet()) {
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            String discordId = entry.getValue();
            player.sendMessage(ChatColor.GREEN + playerName + " - DiscordID: " + discordId);
        }
    }




    public FileConfiguration getPluginConfig() {
        return config;
    }

    public JDA getJda() {
        return jda;
    }

    public Map<UUID, String> getPendingCodes() {
        return pendingCodes;
    }

    public Map<UUID, Boolean> getBypassedPlayers() {
        return bypassedPlayers;
    }

    public Map<UUID, String> getAuthenticatedPlayers() {
        return authenticatedPlayers;
    }

    public Map<String, UUID> getDiscordToMinecraftMap() {
        return discordToMinecraftMap;
    }

    public void addBypassedPlayer(UUID uuid) {
        if (!bypassedPlayers.containsKey(uuid)) {
            // 認証不要ユーザーリストに追加
            bypassedPlayers.put(uuid, true);

            // usersConfig にも即時反映
            List<String> bypassedList = usersConfig.getStringList("bypassed");
            if (!bypassedList.contains(uuid.toString())) {
                bypassedList.add(uuid.toString());
                usersConfig.set("bypassed", bypassedList);
            }
            saveUserData();
        }
    }

    private void synchronizeBypassedList() {
        // users.yml の "bypassed" リストを上書き保存
        List<String> bypassedList = new ArrayList<>();
        for (UUID uuid : bypassedPlayers.keySet()) {
            bypassedList.add(uuid.toString());
        }
        usersConfig.set("bypassed", bypassedList); // 同期処理を明示的に実行
    }

    public void removeBypassedPlayer(UUID uuid) {
        if (bypassedPlayers.containsKey(uuid)) {
            // 認証不要ユーザーリストから削除
            bypassedPlayers.remove(uuid);

            // usersConfig にも即時反映
            List<String> bypassedList = usersConfig.getStringList("bypassed");
            if (bypassedList.contains(uuid.toString())) {
                bypassedList.remove(uuid.toString());
                usersConfig.set("bypassed", bypassedList);
            }
            saveUserData();
        }
    }



    private void loadUserData() {
        usersFile = new File(getDataFolder(), "users.yml");

        if (!usersFile.exists()) {
            getLogger().severe(PREFIX + "users.yml が存在しません！ plugins/DisLink ディレクトリに作成してください！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        usersConfig = YamlConfiguration.loadConfiguration(usersFile);

        // 認証済みユーザーをロード
        if (usersConfig.contains("authenticated")) {
            for (String uuidString : usersConfig.getConfigurationSection("authenticated").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String discordId = usersConfig.getString("authenticated." + uuidString);

                    if (discordId != null && !discordId.isEmpty()) {
                        authenticatedPlayers.put(uuid, discordId);
                        discordToMinecraftMap.put(discordId, uuid);
                    } else {
                        getLogger().warning(PREFIX + "不正な認証データをスキップ: UUID=" + uuidString);
                    }
                } catch (IllegalArgumentException e) {
                    getLogger().warning(PREFIX + "無効なUUID形式をスキップ: " + uuidString);
                }
            }
        }

        // 認証不要ユーザーをロード
        if (usersConfig.contains("bypassed")) {
            List<String> bypassedList = usersConfig.getStringList("bypassed");
            for (String uuidString : bypassedList) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    bypassedPlayers.put(uuid, true);
                } catch (IllegalArgumentException e) {
                    getLogger().warning(PREFIX + "無効なUUID形式をスキップ (bypassed): " + uuidString);
                }
            }
        } else {
            getLogger().info(PREFIX + "認証不要ユーザーのデータが存在しません。");
        }

        getLogger().info(PREFIX + "ユーザーデータをロードしました。");
    }





    public boolean isAuthenticated(UUID uuid) {
        String discordId = usersConfig.getString("authenticated." + uuid.toString());
        return discordId != null && !discordId.trim().isEmpty(); // Trim で空白を防止
    }


    public String getAuthenticatedDiscordId(UUID uuid) {
        return usersConfig.getString("authenticated." + uuid.toString());
    }


    public FileConfiguration getUsersConfig() {
        return usersConfig;
    }



    public void saveUserData() {
        try {
            // 1. 認証済みユーザーを保存
            for (Map.Entry<UUID, String> entry : authenticatedPlayers.entrySet()) {
                usersConfig.set("authenticated." + entry.getKey().toString(), entry.getValue());
            }

            // 2. 認証不要ユーザーを保存
            // "bypassed" セクションのリストを上書き保存
            List<String> bypassedList = new ArrayList<>();
            for (UUID uuid : bypassedPlayers.keySet()) {
                bypassedList.add(uuid.toString());
            }
            usersConfig.set("bypassed", bypassedList);

            usersConfig.save(usersFile);

            // 3. 不要なエントリを削除（条件付きで特定のUUIDを削除）
            String targetUUID = "04e343cf-6397-4131-8d7b-5b82d1228fdc";
            String targetValue = "true";

            // UUIDが存在し、かつ値が 'true' の場合に削除
            if (usersConfig.contains("authenticated." + targetUUID)) {
                String value = usersConfig.getString("authenticated." + targetUUID);
                if (targetValue.equals(value)) {
                    // UUIDと値を削除
                    usersConfig.set("authenticated." + targetUUID, null);
                    authenticatedPlayers.remove(UUID.fromString(targetUUID));
                    getLogger().warning(PREFIX + "不要なUUIDエントリ '" + targetUUID + ": " + targetValue + "' を削除しました。");
                }
            }

            // 4. データをファイルに保存
            usersConfig.save(usersFile);
            getLogger().info(PREFIX + "ユーザーデータを保存しました。");

        } catch (IOException e) {
            getLogger().severe(PREFIX + "ユーザーデータの保存中にエラーが発生しました！");
            e.printStackTrace();
        }
    }

}