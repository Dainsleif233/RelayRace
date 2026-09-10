package top.syshub.relayrace.common.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import org.jetbrains.annotations.Nullable;

/**
 * Public API for external plugins.
 *
 * <p>Obtain via {@link #get()} or {@code plugin.getApi()} after RelayRace is enabled.
 * All methods must be called from the server main thread.
 */
public interface RelayRaceApi {

    /**
     * Looks up the registered RelayRace API from Bukkit ServicesManager.
     *
     * @return the API, or {@code null} if RelayRace is not enabled
     */
    @Nullable
    static RelayRaceApi get() {
        RegisteredServiceProvider<RelayRaceApi> rsp =
            Bukkit.getServicesManager().getRegistration(RelayRaceApi.class);
        return rsp == null ? null : rsp.getProvider();
    }

    /**
     * Whether a relay game is currently running.
     */
    boolean isGameRunning();

    /**
     * The player whose turn is currently active, or {@code null} if none.
     *
     * <p>During an external-lobby bringback rotation this may still be the
     * previous player (the timer is paused until the next player arrives).
     */
    @Nullable
    Player getActivePlayer();

    /**
     * Remaining play time of the current turn, in whole seconds.
     * Returns {@code 0} when no game is running.
     *
     * <p>During freeze countdown or external-lobby bringback this reflects the
     * value prepared for the upcoming / current turn and may not be ticking.
     */
    int getRemainingSeconds();

    /**
     * Adds play time to the current turn.
     *
     * @param seconds positive number of seconds to add
     * @return {@code true} if the remaining time was changed;
     *         {@code false} if no game is running or {@code seconds <= 0}
     */
    boolean addRemainingSeconds(int seconds);

    /**
     * Removes play time from the current turn.
     *
     * <p>If the subtraction brings remaining time to zero, the active player's
     * turn ends immediately (same as a natural timeout), unless a freeze
     * countdown or an external-lobby rotation is still in progress.
     *
     * @param seconds positive number of seconds to subtract
     * @return {@code true} if the remaining time was changed;
     *         {@code false} if no game is running or {@code seconds <= 0}
     */
    boolean subtractRemainingSeconds(int seconds);
}
