package top.syshub.relayrace.classic;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import top.syshub.relayrace.common.LobbyTerrain;

import java.util.Random;

/**
 * Flat lobby terrain generator for legacy (1.16.x) servers.
 *
 * <p>Uses the 1.16 API entry point {@link #generateChunkData(World, Random,
 * int, int, BiomeGrid)}, which must build and return a chunk data object by
 * hand via {@link #createChunkData(World)}.
 */
public final class ClassicLobbyGenerator extends ChunkGenerator {

    @Override
    @NotNull
    public ChunkData generateChunkData(@NotNull World world, @NotNull Random random, int x, int z, @NotNull BiomeGrid biome) {
        ChunkData data = createChunkData(world);
        LobbyTerrain.generate(data);
        return data;
    }
}
