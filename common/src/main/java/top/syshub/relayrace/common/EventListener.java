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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import top.syshub.relayrace.common.api.Platform;

public class EventListener implements Listener {

    private final RelayRacePlugin plugin;
    private final GameManager gameManager;
    private final LobbyManager lobbyManager;
    private final LobbyMessenger lobbyMessenger;
    private final Platform platform;

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

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (gameManager.isRunning() && gameManager.isActivePlayer(player)) {
            return;
        }
        Location lobbySpawn = lobbyManager.getLobbySpawn();
        if (lobbySpawn == null) return;
        platform.setRespawnLocation(player, lobbySpawn);
        event.setRespawnLocation(lobbySpawn);
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

        plugin.debug(plugin.getTranslator().plain("logger.debug.endPortalTrigger",
            player.getName(), String.valueOf(event.getCause()),
            String.valueOf(event.getFrom().getBlockX()),
            String.valueOf(event.getFrom().getBlockY()),
            String.valueOf(event.getFrom().getBlockZ())));
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