package top.syshub.relayRace;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class EventListener implements Listener {

    private final GameManager gameManager;

    public EventListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        gameManager.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameManager.removePlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerPortalTeleport(PlayerTeleportEvent event) {
        if (!gameManager.isRunning()) return;

        Player player = event.getPlayer();
        if (!gameManager.isActivePlayer(player)) return;
        if (event.getFrom().getWorld() == null
            || event.getFrom().getWorld().getEnvironment() != World.Environment.THE_END) return;

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL
            && event.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN) return;

        event.setCancelled(true);
        gameManager.winGame(player, event.getFrom());
    }
}
