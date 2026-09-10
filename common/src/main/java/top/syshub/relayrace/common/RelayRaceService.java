package top.syshub.relayrace.common;

import org.bukkit.entity.Player;

import org.jetbrains.annotations.Nullable;

import top.syshub.relayrace.common.api.RelayRaceApi;

/**
 * Default {@link RelayRaceApi} implementation backed by {@link GameManager}.
 */
public final class RelayRaceService implements RelayRaceApi {

    private final GameManager gameManager;

    public RelayRaceService(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean isGameRunning() {
        return gameManager.isRunning();
    }

    @Override
    @Nullable
    public Player getActivePlayer() {
        return gameManager.getActivePlayer();
    }

    @Override
    public int getRemainingSeconds() {
        return gameManager.getRemainingSeconds();
    }

    @Override
    public boolean addRemainingSeconds(int seconds) {
        return gameManager.addRemainingSeconds(seconds);
    }

    @Override
    public boolean subtractRemainingSeconds(int seconds) {
        return gameManager.subtractRemainingSeconds(seconds);
    }
}
