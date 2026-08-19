package top.syshub.relayrace.classic;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import top.syshub.relayrace.common.api.WorldFactory;

public final class ClassicWorldFactory implements WorldFactory {

    @Override
    public World createLobbyWorld() {
        World world = Bukkit.getWorld("lobby");
        if (world != null) {
            return world;
        }

        WorldCreator creator = WorldCreator.name("lobby");
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);
        creator.type(WorldType.FLAT);
        creator.generatorSettings("{\"layers\":[{\"height\":1,\"block\":\"minecraft:bedrock\"},{\"height\":127,\"block\":\"minecraft:dirt\"},{\"height\":1,\"block\":\"minecraft:grass_block\"}]}");
        return creator.createWorld();
    }
}