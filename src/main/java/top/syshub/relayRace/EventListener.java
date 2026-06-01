package top.syshub.relayRace;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class EventListener implements Listener {

    private final RelayRace plugin;
    private final GameManager gameManager;

    public EventListener(RelayRace plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        gameManager.addSpectator(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameManager.removePlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!gameManager.isRunning()) return;

        Player player = event.getPlayer();
        if (!gameManager.isActivePlayer(player)) return;
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) return;

        Location to = event.getTo();
        if (to.getBlock().getType() != Material.END_PORTAL
            && to.clone().add(0, 1, 0).getBlock().getType() != Material.END_PORTAL) {
            return;
        }

        debug("Move into End portal detected: " + player.getName());
        gameManager.winGame(player, to);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEnderPearl(PlayerTeleportEvent event) {
        if (!gameManager.isRunning()) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;

        Player player = event.getPlayer();
        if (!gameManager.isActivePlayer(player)) return;
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) return;

        Location to = event.getTo();
        if (to.getBlock().getType() != Material.END_PORTAL
            && to.clone().add(0, 1, 0).getBlock().getType() != Material.END_PORTAL) {
            return;
        }

        debug("Ender pearl into End portal blocked: " + player.getName());
        event.setCancelled(true);
        gameManager.winGame(player, to);
    }

    private void debug(String msg) {
        if (gameManager.isDebug()) {
            plugin.getLogger().warning("[DEBUG] " + msg);
        }
    }
}
