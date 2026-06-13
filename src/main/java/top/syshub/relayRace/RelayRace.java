package top.syshub.relayRace;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class RelayRace extends JavaPlugin {

    private Translator translator;
    private GameManager gameManager;
    private LobbyMessenger lobbyMessenger;

    public Translator getTranslator() {
        return translator;
    }

    @Override
    @SuppressWarnings("removal")
    public void onEnable() {
        LobbyManager lobbyManager = new LobbyManager();
        lobbyManager.createLobbyWorld();

        // Initialize translator early so config loading can use localized messages
        translator = new Translator(this);
        translator.loadLocale(getConfig().getString("locale", "zh"));

        // Load config
        RelayRaceConfig config = new RelayRaceConfig(this);
        config.load();

        gameManager = new GameManager(this, lobbyManager, config);

        // Create and wire LobbyMessenger
        lobbyMessenger = new LobbyMessenger(this, gameManager);
        lobbyMessenger.register();
        gameManager.setLobbyMessenger(lobbyMessenger);
        lobbyMessenger.configure(config.getExternalLobbyServer());

        getServer().getPluginManager().registerEvents(new EventListener(this, gameManager, lobbyManager, lobbyMessenger), this);
        CommandHandler.register(this, gameManager);

        // Enable immediate respawn in all loaded worlds so dead players skip the death screen
        for (World world : getServer().getWorlds()) {
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        }

        getLogger().info(translator.translateRaw("logger.plugin.enabled"));
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.disable();
        }
        if (lobbyMessenger != null) {
            lobbyMessenger.unregister();
        }
        getLogger().info(translator != null ? translator.translateRaw("logger.plugin.disabled") : "RelayRace disabled");
    }

    public LobbyMessenger getLobbyMessenger() {
        return lobbyMessenger;
    }

    /**
     * 仅在 debug 模式启用时输出日志。
     * 由 EventListener、LobbyMessenger 等类共用。
     */
    public void debug(String msg) {
        if (gameManager != null && gameManager.getConfig().isDebug()) {
            getLogger().warning("[DEBUG] " + msg);
        }
    }
}
