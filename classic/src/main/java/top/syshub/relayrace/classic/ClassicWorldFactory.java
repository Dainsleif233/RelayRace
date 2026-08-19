package top.syshub.relayrace.classic;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

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
        // 1.16.1's FlatGeneratorInfo does not reliably honour the settings
        // JSON (SPIGOT-5970); use a custom generator so the surface is always
        // at y=64 regardless of the Minecraft version.
        creator.generator(new ClassicLobbyGenerator());
        return creator.createWorld();
    }
}