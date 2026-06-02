package top.syshub.relayRace;

import org.bukkit.plugin.java.JavaPlugin;

public final class RelayRace extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        LobbyManager lobbyManager = new LobbyManager();
        lobbyManager.createLobbyWorld();

        gameManager = new GameManager(this, lobbyManager);
        gameManager.loadConfig();

        getServer().getPluginManager().registerEvents(new EventListener(gameManager), this);
        CommandHandler.register(this, gameManager);

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
