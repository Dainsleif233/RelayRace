package top.syshub.relayrace.common;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;

/**
 * Shared flat lobby terrain layout, independent of server version.
 *
 * <p>The ground surface is always at {@value #SURFACE_Y} and the layers below
 * are: bedrock at y=0, dirt from y=1 to y=62, and a grass block at y=63 whose
 * top face is the walkable surface at y=64.
 */
public final class LobbyTerrain {

    /** Y of the top face of the ground (walkable surface). */
    public static final int SURFACE_Y = 64;

    /** Y of the only grass block covering the surface. */
    private static final int GRASS_Y = SURFACE_Y - 1;

    /** Y of the single bedrock layer at the bottom. */
    private static final int BEDROCK_Y = 0;

    private static final int DIRT_TOP_Y = GRASS_Y; // exclusive upper bound

    private LobbyTerrain() {
    }

    /**
     * Fills a chunk with the flat lobby layout. Implementations of
     * object created for the chunk being generated.
     */
    public static void generate(ChunkGenerator.ChunkData data) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                data.setBlock(x, BEDROCK_Y, z, Material.BEDROCK);
                for (int y = BEDROCK_Y + 1; y < DIRT_TOP_Y; y++) {
                    data.setBlock(x, y, z, Material.DIRT);
                }
                data.setBlock(x, GRASS_Y, z, Material.GRASS_BLOCK);
            }
        }
    }
}