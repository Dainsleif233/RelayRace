package top.syshub.relayRace;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public class LobbyManager {

    private final JavaPlugin plugin;
    private World lobbyWorld;
    private final NamespacedKey LOBBY_KEY = new NamespacedKey("relayrace", "lobby");

    public LobbyManager(JavaPlugin plugin) {
        this.plugin = plugin;
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
        int topY = lobbyWorld.getHighestBlockYAt(0, 0);
        lobbyWorld.setSpawnLocation(0, topY + 1, 0);
    }

    public World getLobbyWorld() {
        return lobbyWorld;
    }

    public void teleportToLobby(Player player) {
        Location spawn = lobbyWorld.getSpawnLocation();
        player.setRespawnLocation(spawn, true);
        player.teleport(spawn);
    }
}
