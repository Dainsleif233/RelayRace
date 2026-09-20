package top.syshub.relayrace.common.replay;

import dev.zeffut.flashbackserver.api.FlashbackAPI;
import dev.zeffut.flashbackserver.api.RecordingService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Soft-dependency bridge for FlashbackServer.
 * <p>
 * Never touch {@link FlashbackAPI} unless the plugin is installed — otherwise
 * the class is missing at runtime (including on classic 1.16 servers).
 * API jar is compileOnly and is never shaded.
 */
public final class FlashbackReplayRecorder {

    private static final String PLUGIN_NAME = "FlashbackServer";

    private static Logger log() {
        return Bukkit.getLogger();
    }

    private static boolean isPluginInstalled() {
        return Bukkit.getPluginManager().getPlugin(PLUGIN_NAME) != null;
    }

    public boolean isAvailable() {
        return isPluginInstalled() && FlashbackAPI.isAvailable();
    }

    public boolean start(Player player) {
        if (player == null || !isAvailable()) {
            return false;
        }
        try {
            // Re-fetch on each use; do not cache across plugin reloads.
            RecordingService recording = FlashbackAPI.recording();
            return recording != null && recording.start(player);
        } catch (Throwable t) {
            log().warning("[RelayRace] Flashback start failed for " + player.getName() + ": " + t);
            return false;
        }
    }

    public void stop(Player player, Path target) {
        if (player == null || target == null || !isAvailable()) {
            return;
        }
        try {
            RecordingService recording = FlashbackAPI.recording();
            if (recording == null || !recording.isRecording(player)) {
                return;
            }
            Path absolute = target.toAbsolutePath();
            recording.stop(player, absolute).whenComplete((path, ex) -> {
                if (ex != null) {
                    log().warning("[RelayRace] Flashback save failed for "
                        + player.getName() + ": " + ex);
                }
            });
        } catch (Throwable t) {
            log().warning("[RelayRace] Flashback stop failed for " + player.getName() + ": " + t);
        }
    }
}
