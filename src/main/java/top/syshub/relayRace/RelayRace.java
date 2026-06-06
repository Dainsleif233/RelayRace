package top.syshub.relayRace;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class RelayRace extends JavaPlugin {

    private Translator translator;
    private GameManager gameManager;

    public Translator getTranslator() {
        return translator;
    }

    @Override
    @SuppressWarnings("removal")
    public void onEnable() {
        LobbyManager lobbyManager = new LobbyManager();
        lobbyManager.createLobbyWorld();

        gameManager = new GameManager(this, lobbyManager);
        gameManager.loadConfig();

        translator = new Translator(this);
        translator.loadLocale(getConfig().getString("locale", "zh"));

        getServer().getPluginManager().registerEvents(new EventListener(gameManager, lobbyManager), this);
        CommandHandler.register(this, gameManager);

        // Enable immediate respawn in all loaded worlds so dead players skip the death screen
        for (World world : getServer().getWorlds()) {
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        }

        getLogger().info("RelayRace enabled");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.disable();
        }
        getLogger().info("RelayRace disabled");
    }
}
