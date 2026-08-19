package top.syshub.relayrace.common;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import top.syshub.relayrace.common.api.WorldFactory;

public class LobbyManager {

    private final WorldFactory worldFactory;
    private World lobbyWorld;

    public LobbyManager(WorldFactory worldFactory) {
        this.worldFactory = worldFactory;
    }

    public void createLobbyWorld() {
        lobbyWorld = worldFactory.createLobbyWorld();
        if (lobbyWorld != null) {
            lobbyWorld.setSpawnLocation(0, 65, 0);
        }
    }

    public void teleportToLobby(Player player) {
        if (lobbyWorld == null) return;
        player.teleport(new Location(lobbyWorld, 0.5, 65, 0.5));
        player.setFallDistance(0);
    }

    public Location getLobbySpawn() {
        if (lobbyWorld == null) return null;
        return new Location(lobbyWorld, 0.5, 65, 0.5);
    }

    public World getLobbyWorld() {
        return lobbyWorld;
    }
}