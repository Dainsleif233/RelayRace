package top.syshub.relayrace.common.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import top.syshub.relayrace.common.GameManager;
import top.syshub.relayrace.common.RelayRacePlugin;

/**
 * Version-specific platform facade.
 * <p>
 * Common game code depends only on this interface. Each supported server
 * version provides one implementation (latest / classic).
 */
public interface Platform {

    String id();

    CommandRegistrar commands();

    Scheduler scheduler();

    TickControl tickControl();

    WorldFactory worldFactory();

    PlayerUi ui();

    /**
     * Marks the win-screen state. This API does not exist on older Bukkit/Paper
     * servers, so classic keeps it as a no-op.
     */
    default void setHasSeenWinScreen(Player player, boolean value) {
    }

    /**
     * Capture version-specific player state that is not available on every
     * server API (for example arrows-in-body and freeze ticks on Paper 26.2).
     */
    default void capturePlayerExtras(Player player, PlayerExtras extras) {
    }

    /**
     * Apply version-specific player state captured by
     * {@link #capturePlayerExtras(Player, PlayerExtras)}.
     */
    default void applyPlayerExtras(Player player, PlayerExtras extras) {
    }

    /**
     * Clear version-specific player state after it has been captured.
     */
    default void resetPlayerExtras(Player player) {
    }

    /**
     * Capture the player's total experience points. Latest can use the more
     * precise Paper method when available; classic uses the common Bukkit API.
     */
    default int captureTotalExperience(Player player) {
        return player.getTotalExperience();
    }

    /**
     * Set the player's respawn point. Latest uses the newer method when
     * available; classic falls back to the legacy bed-spawn API.
     */
    default void setRespawnLocation(Player player, Location location) {
        player.setBedSpawnLocation(location, true);
    }

    /**
     * Capture the player's max health. Latest can use the attribute API;
     * classic falls back to the deprecated but still available method.
     */
    @SuppressWarnings("deprecation")
    default double captureMaxHealth(Player player) {
        return player.getMaxHealth();
    }

    /**
     * Apply the player's max health. Latest can use the attribute API;
     * classic falls back to the deprecated but still available method.
     */
    @SuppressWarnings("deprecation")
    default void applyMaxHealth(Player player, double maxHealth) {
        player.setMaxHealth(maxHealth);
    }

    default void registerVersionEvents(RelayRacePlugin plugin, GameManager gameManager) {
    }
}