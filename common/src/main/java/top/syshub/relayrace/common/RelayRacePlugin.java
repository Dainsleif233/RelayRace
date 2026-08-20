package top.syshub.relayrace.common;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import top.syshub.relayrace.common.api.Platform;

public final class RelayRacePlugin extends JavaPlugin {

    private Translator translator;
    private GameManager gameManager;
    private LobbyMessenger lobbyMessenger;
    private RelayRaceConfig config;
    private Platform platform;

    public Translator getTranslator() {
        return translator;
    }

    public RelayRaceConfig getRelayConfig() {
        return config;
    }

    public Platform getPlatform() {
        return platform;
    }

    @Override
    public void onLoad() {
        // Resolve the version platform as early as possible so that
        // version-specific bootstrap hooks (e.g. the CommandAPI initializer on
        // the classic/1.16 branch) run before onEnable().
        if (platform == null) {
            platform = PlatformFactory.load(this);
        }
        if (platform != null) {
            platform.onLoad(this);
        }
    }

    @Override
    public void onEnable() {
        if (platform == null) {
            platform = PlatformFactory.load(this);
        }
        if (platform == null) {
            getLogger().severe("Unsupported server version: " + getServer().getBukkitVersion());
            setEnabled(false);
            return;
        }

        LobbyManager lobbyManager = new LobbyManager(platform.worldFactory());
        lobbyManager.createLobbyWorld();

        translator = new Translator(this);
        String locale = getConfig().getString("locale", "zh");
        translator.loadLocale(locale);

        config = new RelayRaceConfig(this, platform);
        config.load();

        gameManager = new GameManager(this, lobbyManager, config, platform);

        lobbyMessenger = new LobbyMessenger(this, gameManager, platform);
        lobbyMessenger.register();
        gameManager.setLobbyMessenger(lobbyMessenger);
        lobbyMessenger.configure(config.getExternalLobbyServer());

        getServer().getPluginManager().registerEvents(
            new EventListener(this, gameManager, lobbyManager, lobbyMessenger, platform), this);

        platform.commands().register(this, gameManager);
        platform.registerVersionEvents(this, gameManager);

        for (World world : getServer().getWorlds()) {
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        }

        getLogger().info(translator.plain("logger.plugin.enabled"));
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.disable();
        }
        if (lobbyMessenger != null) {
            lobbyMessenger.unregister();
        }
        getLogger().info(translator != null
            ? translator.plain("logger.plugin.disabled")
            : "RelayRace disabled");
    }

    public void debug(String msg) {
        if (getRelayConfig() != null && getRelayConfig().isDebug()) {
            getLogger().warning("[DEBUG] " + msg);
        }
    }
}