package top.syshub.relayrace.latest;

import org.bukkit.HeightMap;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import org.jspecify.annotations.NonNull;
import top.syshub.relayrace.common.LobbyTerrain;

import java.util.Random;

/**
 * Flat lobby terrain generator for modern (Paper) servers.
 *
 * <p>Modern Paper splits chunk generation into noise/surface/bedrock/caves
 * stages; the legacy {@code generateChunkData} entry point is deprecated and
 * no longer called. The whole flat layout is written in the noise stage, and
 * vanilla surface/bedrock/caves stay disabled by default.
 */
public final class LatestLobbyGenerator extends ChunkGenerator {

    @Override
    public void generateNoise(@NonNull WorldInfo worldInfo, @NonNull Random random, int chunkX, int chunkZ, @NonNull ChunkData chunkData) {
        LobbyTerrain.generate(chunkData);
    }

    @Override
    public int getBaseHeight(@NonNull WorldInfo worldInfo, @NonNull Random random, int x, int z, @NonNull HeightMap heightMap) {
        return LobbyTerrain.SURFACE_Y;
    }
}
