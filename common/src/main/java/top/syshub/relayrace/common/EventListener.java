package top.syshub.relayrace.common;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import top.syshub.relayrace.common.api.Platform;

import java.util.UUID;

public class EventListener implements Listener {

    private final RelayRacePlugin plugin;
    private final GameManager gameManager;
    private final LobbyManager lobbyManager;
    private final LobbyMessenger lobbyMessenger;
    private final Platform platform;

    // Tracks the UUID of the active player who just died, so that the
    // subsequent PlayerRespawnEvent can distinguish a death-respawn from
    // a credits-respawn (End exit portal). On 1.16.1 the End exit portal
    // never fires PlayerTeleportEvent — it always goes through the
    // credits→respawn path, so we detect the win here instead.
    private UUID lastDeathUuid;

    public EventListener(RelayRacePlugin plugin, GameManager gameManager,
                         LobbyManager lobbyManager, LobbyMessenger lobbyMessenger,
                         Platform platform) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.lobbyManager = lobbyManager;
        this.lobbyMessenger = lobbyMessenger;
        this.platform = platform;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        gameManager.handlePlayerJoin(player);
        lobbyMessenger.tryAutoDetect(player);
        lobbyMessenger.notifyArrived(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameManager.removePlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (gameManager.isRunning() && gameManager.isActivePlayer(event.getEntity())) {
            lastDeathUuid = event.getEntity().getUniqueId();
            plugin.debug("[death] active player died: " + event.getEntity().getName());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (gameManager.isRunning() && gameManager.isActivePlayer(player)) {
            plugin.debug("[respawn] active player respawn, lastDeathUuid=" + lastDeathUuid);
            if (lastDeathUuid != null && lastDeathUuid.equals(player.getUniqueId())) {
                // Death respawn — clear flag, let the normal bed-spawn proceed.
                lastDeathUuid = null;
                plugin.debug("[respawn] death respawn, returning");
                return;
            }
            // No preceding death → credits respawn from the End exit portal.
            // On 1.16.1 the exit portal always takes the credits→respawn path
            // (never PlayerTeleportEvent) and PlayerRespawnEvent cannot be
            // canceled. Respawn back at the same spot so the player stays in
            // the End (matching the 26.x behavior of cancelling the portal
            // teleport), destroy the portal blocks to prevent an immediate
            // re-trigger, then defer winGame a few ticks so the respawn
            // world-reload does not clear the victory title endGame sends.
            plugin.debug("[respawn] credits respawn detected, scheduling winGame");
            Location endLoc = player.getLocation();
            gameManager.destroyEndPortalBlocks(endLoc);
            event.setRespawnLocation(endLoc);
            platform.scheduler().runDelayed(plugin, () -> gameManager.winGame(player, null), 2L);
            return;
        }
        Location lobbySpawn = lobbyManager.getLobbySpawn();
        if (lobbySpawn == null) return;
        platform.setRespawnLocation(player, lobbySpawn);
        event.setRespawnLocation(lobbySpawn);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerPortalTeleport(PlayerTeleportEvent event) {
        plugin.debug("[portal] player=" + event.getPlayer().getName() + " cause=" + event.getCause() + " from=" + event.getFrom() + " to=" + event.getTo());
        if (!gameManager.isRunning()) {
            plugin.debug("[portal] early return - game not running");
            return;
        }

        Player player = event.getPlayer();
        if (!gameManager.isActivePlayer(player)) {
            plugin.debug("[portal] early return - not active player");
            return;
        }
        if (event.getFrom().getWorld() == null
            || event.getFrom().getWorld().getEnvironment() != World.Environment.THE_END) {
            plugin.debug("[portal] early return - not from THE_END (fromWorld=" + (event.getFrom().getWorld() != null ? event.getFrom().getWorld().getName() + "/" + event.getFrom().getWorld().getEnvironment() : "null") + ")");
            return;
        }

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL
            && event.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            plugin.debug("[portal] early return - cause not END_PORTAL/UNKNOWN");
            return;
        }

        plugin.debug(plugin.getTranslator().plain("logger.debug.endPortalTrigger",
            player.getName(), String.valueOf(event.getCause()),
            String.valueOf(event.getFrom().getBlockX()),
            String.valueOf(event.getFrom().getBlockY()),
            String.valueOf(event.getFrom().getBlockZ())));
        plugin.debug("[portal] WIN detected! player=" + player.getName() + " cause=" + event.getCause() + " from=" + event.getFrom());
        event.setCancelled(true);
        gameManager.winGame(player, event.getFrom());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        if (event.getOwner() instanceof Player) {
            Player player = (Player) event.getOwner();
            gameManager.onEntityTame(player.getUniqueId(), event.getEntity().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Tameable) {
            gameManager.onEntityDeath(event.getEntity().getUniqueId());
        }
    }
}