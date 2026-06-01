package top.syshub.relayRace;

import org.bukkit.plugin.java.JavaPlugin;

public final class RelayRace extends JavaPlugin {

    private GameManager gameManager;
    private LobbyManager lobbyManager;

    @Override
    public void onEnable() {
        lobbyManager = new LobbyManager(this);
        lobbyManager.createLobbyWorld();

        gameManager = new GameManager(this, lobbyManager);
        gameManager.loadConfig();

        getServer().getPluginManager().registerEvents(new EventListener(this, gameManager), this);
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

    public GameManager getGameManager() {
        return gameManager;
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }
}
