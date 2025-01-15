package shake_1227.dislink;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import shake_1227.dislink.commands.BypassCommand;
import shake_1227.dislink.commands.ReauthCommand;
import shake_1227.dislink.gui.DisLinkGUI;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DisLink extends JavaPlugin {

    private JDA jda;
    private FileConfiguration config;

    // 認証コードの保留リスト (認証待ち)
    private final Map<UUID, String> pendingCodes = new HashMap<>();

    // 認証不要ユーザーのリスト (認証が不要なプレイヤー)
    private final Map<UUID, Boolean> bypassedPlayers = new HashMap<>();

    // 認証済みユーザーのリスト (認証を完了したプレイヤー)
    private final Map<UUID, Boolean> authenticatedPlayers = new HashMap<>();

    // DiscordアカウントとMinecraft UUIDのマッピング
    private final Map<String, UUID> discordToMinecraftMap = new HashMap<>();

    public static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.LIGHT_PURPLE + "Dis" + ChatColor.YELLOW + "Link" + ChatColor.GRAY + "] ";

    @Override
    public void onEnable() {
        // 設定ファイルを読み込む
        saveDefaultConfig();
        this.config = getConfig();

        // リスナーを登録
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // コマンドを登録
        getCommand("bypass").setExecutor(new BypassCommand(this));
        getCommand("reauth").setExecutor(new ReauthCommand(this));

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
        getLogger().info(PREFIX + "プラグインが無効化されました！");
    }

    private void initializeDiscordBot() throws LoginException {
        // config.yml からボットトークンを取得
        String botToken = config.getString("discord.bot-token");
        if (botToken == null || botToken.isEmpty()) {
            throw new IllegalArgumentException(PREFIX + "Discord Bot トークンが設定されていません！ config.yml を確認してください。");
        }

        // JDAの初期化: MESSAGE_CONTENT ゲートウェイインテントを有効化
        jda = JDABuilder.createDefault(botToken)
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,    // サーバーメッセージの受信
                        GatewayIntent.MESSAGE_CONTENT    // メッセージ内容の取得
                )
                .addEventListeners(new DiscordListener(this)) // Discordイベントリスナーを追加
                .build();

        // 初期化成功メッセージ
        getLogger().info(PREFIX + "Discord Bot が正常に初期化されました！");
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

    public Map<UUID, Boolean> getAuthenticatedPlayers() {
        return authenticatedPlayers;
    }

    public Map<String, UUID> getDiscordToMinecraftMap() {
        return discordToMinecraftMap;
    }

    /**
     * プレイヤーが認証済みかどうかを確認
     *
     * @param uuid プレイヤーのUUID
     * @return 認証済みの場合はtrue、そうでない場合はfalse
     */
    public boolean isAuthenticated(UUID uuid) {
        return authenticatedPlayers.containsKey(uuid) && authenticatedPlayers.get(uuid);
    }

    /**
     * プレイヤーが認証不要ユーザーかどうかを確認
     *
     * @param uuid プレイヤーのUUID
     * @return 認証不要ユーザーの場合はtrue、そうでない場合はfalse
     */
    public boolean isBypassed(UUID uuid) {
        return bypassedPlayers.containsKey(uuid) && bypassedPlayers.get(uuid);
    }

    /**
     * プレイヤーを認証済みリストに追加
     *
     * @param uuid プレイヤーのUUID
     */
    public void addAuthenticatedPlayer(UUID uuid) {
        authenticatedPlayers.put(uuid, true);
    }

    /**
     * プレイヤーを認証不要リストに追加
     *
     * @param uuid プレイヤーのUUID
     */
    public void addBypassedPlayer(UUID uuid) {
        bypassedPlayers.put(uuid, true);
    }

    /**
     * プレイヤーを認証済みリストから削除
     *
     * @param uuid プレイヤーのUUID
     */
    public void removeAuthenticatedPlayer(UUID uuid) {
        authenticatedPlayers.remove(uuid);
    }

    /**
     * プレイヤーを認証不要リストから削除
     *
     * @param uuid プレイヤーのUUID
     */
    public void removeBypassedPlayer(UUID uuid) {
        bypassedPlayers.remove(uuid);
    }
}
