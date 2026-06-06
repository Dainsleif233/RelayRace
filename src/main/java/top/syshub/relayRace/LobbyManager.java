package top.syshub.relayRace;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class LobbyManager {

    private World lobbyWorld;
    private final NamespacedKey LOBBY_KEY = new NamespacedKey("relayrace", "lobby");

    public LobbyManager() {
    }

    public void createLobbyWorld() {
        World world = Bukkit.getWorld(LOBBY_KEY);
        if (world != null) {
            lobbyWorld = world;
            return;
        }
        WorldCreator creator = WorldCreator.name("lobby");
        try {
            Field keyField = WorldCreator.class.getDeclaredField("key");
            keyField.setAccessible(true);
            keyField.set(creator, LOBBY_KEY);
        } catch (Exception ignored) {
        }
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);
        creator.type(WorldType.FLAT);
        creator.generatorSettings("{\"layers\":[{\"height\":1,\"block\":\"minecraft:bedrock\"},{\"height\":127,\"block\":\"minecraft:dirt\"},{\"height\":1,\"block\":\"minecraft:grass_block\"}]}");
        lobbyWorld = creator.createWorld();
        if (lobbyWorld != null) {
            lobbyWorld.setSpawnLocation(0, 65, 0);
        }
    }

    public void teleportToLobby(Player player) {
        player.teleport(new Location(lobbyWorld, 0.5, 65, 0.5));
        player.setFallDistance(0);
    }

    public Location getLobbySpawn() {
        return new Location(lobbyWorld, 0.5, 65, 0.5);
    }

    public World getLobbyWorld() {
        return lobbyWorld;
    }
}
