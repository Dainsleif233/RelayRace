package top.syshub.relayrace.latest;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import top.syshub.relayrace.common.api.WorldFactory;

import java.lang.reflect.Field;

public final class LatestWorldFactory implements WorldFactory {

    @Override
    public World createLobbyWorld() {
        NamespacedKey key = new NamespacedKey("relayrace", "lobby");
        World world = Bukkit.getWorld(key);
        if (world != null) {
            return world;
        }

        WorldCreator creator = WorldCreator.name("lobby");
        try {
            Field keyField = WorldCreator.class.getDeclaredField("key");
            keyField.setAccessible(true);
            keyField.set(creator, key);
        } catch (Exception ignored) {
        }
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);
        creator.generator(new LatestLobbyGenerator());
        return creator.createWorld();
    }
}
