package top.syshub.relayRace;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class GameManager {

    private final RelayRace plugin;
    private final LobbyManager lobbyManager;
    private final RelayRaceConfig config;
    private GameState gameState = GameState.IDLE;

    private int remainingTicks;

    private Player activePlayer;
    private final List<Player> waitingPlayers = new ArrayList<>();
    private final Set<UUID> offlineWaiting = new HashSet<>();

    private BossBar bossBar;
    private ScheduledTask timerTask;
    private ScheduledTask countdownTask;

    private Team greenTeam;
    private Team yellowTeam;
    private float previousTickRate = 20.0f;
    private boolean countdownActive = false;

    private long gameStartTime;

    private LobbyMessenger lobbyMessenger;

    /** Holds state for an asynchronous player rotation (external lobby bring-back). */
    private PendingRotation pendingRotation;

    /** Last second at which a waiting reminder was sent. Used in tick(). */
    private int lastReminderCheck = -1;

    // Pet ownership reverse index: player UUID → owned tameable entity UUIDs.
    // Populated by EntityTameEvent and synced with world scans during player switch.
    private final Map<UUID, Set<UUID>> petOwnershipIndex = new HashMap<>();
    // Deferred pet transfers: entity UUID → new owner UUID, for entities in unloaded chunks.
    // Applied when the chunk loads via EntitiesLoadEvent.
    private final Map<UUID, UUID> pendingPetTransfers = new HashMap<>();

    /**
     * Holds data needed to complete a player rotation that was deferred while waiting
     * for a player to be brought back from the external lobby.
     */
    private static class PendingRotation {
        final UUID playerUuid;
        final PlayerData snapshot;
        final Player oldActive;
        final Entity oldVehicle;
        final List<Entity> oldPassengers;
        final Entity oldShoulderLeft;
        final Entity oldShoulderRight;
        volatile boolean cancelled;

        PendingRotation(UUID playerUuid, PlayerData snapshot, Player oldActive,
                        Entity oldVehicle, List<Entity> oldPassengers,
                        Entity oldShoulderLeft, Entity oldShoulderRight) {
            this.playerUuid = playerUuid;
            this.snapshot = snapshot;
            this.oldActive = oldActive;
            this.oldVehicle = oldVehicle;
            this.oldPassengers = oldPassengers;
            this.oldShoulderLeft = oldShoulderLeft;
            this.oldShoulderRight = oldShoulderRight;
        }
    }

    public GameManager(RelayRace plugin, LobbyManager lobbyManager, RelayRaceConfig config) {
        this.plugin = plugin;
        this.lobbyManager = lobbyManager;
        this.config = config;
        setupTeams();
    }

    public Translator getTranslator() {
        return plugin.getTranslator();
    }

    public RelayRaceConfig getConfig() {
        return config;
    }

    public void setLobbyMessenger(LobbyMessenger lobbyMessenger) {
        this.lobbyMessenger = lobbyMessenger;
    }

    /** @return the currently active player, or null if the game is not running. */
    public Player getActivePlayer() {
        return activePlayer;
    }

    // --- Config convenience (config + game-state side effects) ---

    public void setPlaytimeSeconds(int seconds) {
        int newTicks = seconds * 20;
        config.setPlaytimeSeconds(seconds);
        if (remainingTicks > newTicks) {
            remainingTicks = newTicks;
        }
    }

    public void setExternalLobby(boolean value) {
        config.setExternalLobby(value);
        if (lobbyMessenger != null) {
            lobbyMessenger.configure(config.getExternalLobbyServer());
        }
    }

    public void setExternalLobbyServer(String server) {
        config.setExternalLobbyServer(server);
        if (lobbyMessenger != null) {
            lobbyMessenger.configure(server);
        }
    }

    // --- Queries ---

    public boolean isRunning() {
        return gameState == GameState.RUNNING;
    }

    public boolean isActivePlayer(Player player) {
        return player.equals(activePlayer);
    }

    public boolean isWaiting(Player player) {
        return waitingPlayers.contains(player);
    }

    /** Player is either active or in the waiting queue. */
    public boolean isISpec(Player player) {
        return !isActivePlayer(player) && !isWaiting(player);
    }

    // --- Teams ---

    private void setupTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        greenTeam = scoreboard.getTeam("RR_Green");
        if (greenTeam == null) {
            greenTeam = scoreboard.registerNewTeam("RR_Green");
        }
        greenTeam.setColor(ChatColor.GREEN);
        greenTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        yellowTeam = scoreboard.getTeam("RR_Yellow");
        if (yellowTeam == null) {
            yellowTeam = scoreboard.registerNewTeam("RR_Yellow");
        }
        yellowTeam.setColor(ChatColor.YELLOW);
        yellowTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
    }

    private void assignTeam(Player player, Team team) {
        greenTeam.removePlayer(player);
        yellowTeam.removePlayer(player);
        team.addPlayer(player);
    }

    // --- Display ---

    /** Active player: green bold. */
    private void applyActiveDisplay(Player player) {
        Component name = Component.text(player.getName(), NamedTextColor.GREEN, TextDecoration.BOLD);
        player.playerListName(name);
        player.displayName(name);
    }

    /** Waiting players: yellow [N] prefix, first player bold. */
    private void updateWaitingPrefixes() {
        for (int i = 0; i < waitingPlayers.size(); i++) {
            Player p = waitingPlayers.get(i);
            if (i == 0) {
                Component name = Component.text("[1] " + p.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD);
                p.playerListName(name);
                p.displayName(name);
            } else {
                Component name = Component.text("[" + (i + 1) + "] " + p.getName(), NamedTextColor.YELLOW);
                p.playerListName(name);
                p.displayName(name);
            }
        }
    }

    /** Clear display name overrides (returns to default). */
    private void clearPlayerDisplay(Player player) {
        player.playerListName(null);
        player.displayName(null);
    }

    // --- Boss Bar ---

    private void createBossBar() {
        NamespacedKey key = new NamespacedKey(plugin, "remaining_time");
        bossBar = Bukkit.createBossBar(key, formattedTimeString(), BarColor.GREEN, BarStyle.SEGMENTED_10);
        bossBar.setVisible(true);
    }

    private void updateBossBar() {
        if (bossBar == null) return;
        bossBar.setTitle(formattedTimeString());
        float progress = config.getTurnDuration() > 0 ? (float) remainingTicks / config.getTurnDuration() : 0;
        bossBar.setProgress(Math.clamp(progress, 0, 1));
        if (progress < 0.25) {
            bossBar.setColor(BarColor.RED);
        } else if (progress < 0.5) {
            bossBar.setColor(BarColor.YELLOW);
        } else {
            bossBar.setColor(BarColor.GREEN);
        }
    }

    private String formattedTimeString() {
        int remainingSec = remainingTicks / 20;
        int remMin = remainingSec / 60;
        int remSec = remainingSec % 60;

        long elapsed = System.currentTimeMillis() - gameStartTime;
        int elapsedSec = (int) (elapsed / 1000);
        int elaMin = elapsedSec / 60;
        int elaSec = elapsedSec % 60;

        return plugin.getTranslator().translateRaw("game.bossbar.title",
            String.format("%d:%02d", elaMin, elaSec),
            String.format("%d:%02d", remMin, remSec));
    }

    public void addPlayerToBossBar(Player player) {
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }

    public void removePlayerFromBossBar(Player player) {
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    // --- Group management ---

    /**
     * Handle a player joining the server.
     * Before game: spawn in lobby, unassigned, don't change gamemode.
     * During game: spawn in lobby, unassigned, set to spectator.
     * <p>
     * If the player was in the waiting queue while offline, restore their waiting
     * state and preserve their queue position.
     */
    public void handlePlayerJoin(Player player) {
        // Check if this player was in the waiting queue while offline
        if (offlineWaiting.remove(player.getUniqueId())) {
            // Replace stale Player reference in waitingPlayers with the new one
            for (int i = 0; i < waitingPlayers.size(); i++) {
                if (waitingPlayers.get(i).getUniqueId().equals(player.getUniqueId())) {
                    waitingPlayers.set(i, player);
                    break;
                }
            }
            // Restore waiting state
            assignTeam(player, yellowTeam);
            updateWaitingPrefixes();
            addPlayerToBossBar(player);
            lobbyManager.teleportToLobby(player);
            player.setGameMode(GameMode.ADVENTURE);

            // Notify messenger that a bring-back player has arrived
            if (lobbyMessenger != null) {
                lobbyMessenger.notifyArrived(player.getUniqueId());
            }
            return;
        }

        removeFromAllGroups(player);
        lobbyManager.teleportToLobby(player);
        if (isRunning()) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    /**
     * Add a player to the waiting queue.
     * Before game: just mark as waiting. Appended at end with correct index.
     * During game: mark as waiting, teleport to lobby, adventure mode. Appended at end.
     */
    public void addToWaiting(Player player) {
        removeFromAllGroups(player);
        waitingPlayers.add(player);
        assignTeam(player, yellowTeam);
        addPlayerToBossBar(player);

        if (isRunning()) {
            lobbyManager.teleportToLobby(player);
            player.setGameMode(GameMode.ADVENTURE);
        }

        // Always update display (yellow, or yellow + [N] if sorted)
        updateWaitingPrefixes();
    }

    /**
     * Remove a player from the waiting queue.
     * Before game: just remove the waiting mark.
     * During game: leave the player at their location, set to spectator mode.
     */
    public void removeFromWaiting(Player player) {
        waitingPlayers.remove(player);
        yellowTeam.removePlayer(player);
        clearPlayerDisplay(player);

        if (isRunning()) {
            player.setGameMode(GameMode.SPECTATOR);
        }

        updateWaitingPrefixes();
    }

    /** Remove player from all tracked groups. */
    private void removeFromAllGroups(Player player) {
        waitingPlayers.removeIf(p -> p.getUniqueId().equals(player.getUniqueId()));
        greenTeam.removePlayer(player);
        yellowTeam.removePlayer(player);
        clearPlayerDisplay(player);
        if (activePlayer != null && activePlayer.equals(player)) {
            activePlayer = null;
        }
        offlineWaiting.remove(player.getUniqueId());
    }

    // --- Sorting ---

    public void sortWaiting() {
        Collections.shuffle(waitingPlayers);
        updateWaitingPrefixes();
    }

    // --- Countdown / Freeze ---

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        countdownActive = false;
        Bukkit.getServerTickManager().setTickRate(previousTickRate);
    }

    private void startCountdown(int seconds, Player target, Runnable onComplete) {
        cancelCountdown();
        previousTickRate = Bukkit.getServerTickManager().getTickRate();
        Bukkit.getServerTickManager().setTickRate(1.0f);

        final int[] remaining = {seconds};

        countdownActive = true;

        countdownTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            if (remaining[0] > 0) {
                Title title = Title.title(
                    plugin.getTranslator().translate("game.countdown.number", String.valueOf(remaining[0])),
                    Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(100))
                );
                target.showTitle(title);
                remaining[0]--;
            } else {
                task.cancel();
                countdownTask = null;
                countdownActive = false;
                Bukkit.getServerTickManager().setTickRate(previousTickRate);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }, 1L, 1L);
    }

    // --- Game lifecycle ---

    public boolean startGame() {
        if (isRunning()) {
            return false;
        }
        if (waitingPlayers.isEmpty()) {
            return false;
        }

        setupTeams();
        gameState = GameState.RUNNING;
        gameStartTime = System.currentTimeMillis();

        // Move first waiting player to active
        activePlayer = waitingPlayers.removeFirst();
        assignTeam(activePlayer, greenTeam);
        activePlayer.setGameMode(GameMode.SURVIVAL);

        Location worldSpawn = Bukkit.getWorlds().getFirst().getSpawnLocation();
        worldSpawn.getWorld().setFullTime(0);
        activePlayer.teleport(worldSpawn);
        activePlayer.setRespawnLocation(worldSpawn, true);

        // Reset to initial state
        PlayerData.reset(activePlayer);
        activePlayer.setHasSeenWinScreen(true);
        double maxHealth = activePlayer.getAttribute(Attribute.MAX_HEALTH) != null
            ? Objects.requireNonNull(activePlayer.getAttribute(Attribute.MAX_HEALTH)).getBaseValue() : 20.0;
        activePlayer.setHealth(maxHealth);
        activePlayer.setFoodLevel(20);
        activePlayer.setSaturation(5);

        applyActiveDisplay(activePlayer);

        // Waiting players stay in lobby, adventure mode
        for (Player p : waitingPlayers) {
            p.setGameMode(GameMode.ADVENTURE);
            lobbyManager.teleportToLobby(p);
        }
        updateWaitingPrefixes();

        // All other online players -> spectator mode
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isISpec(p)) {
                p.setGameMode(GameMode.SPECTATOR);
            }
        }

        // Boss bar
        remainingTicks = config.getTurnDuration();
        createBossBar();
        updateBossBar();
        for (Player p : Bukkit.getOnlinePlayers()) {
            addPlayerToBossBar(p);
        }

        // Freeze + 15s countdown, then start timer (skipped when freeze is disabled)
        if (config.isFreeze()) {
            startCountdown(15, activePlayer, () -> {
                Title goTitle = Title.title(
                    plugin.getTranslator().translate("game.go.title"),
                    Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(300))
                );
                activePlayer.showTitle(goTitle);
                startTimer();
            });
        } else {
            startTimer();
        }
        return true;
    }

    /** @return true if a deferred player rotation (external lobby bring-back) is in progress. */
    public boolean isPendingRotation() {
        return pendingRotation != null;
    }

    public void switchToNextPlayer() {
        if (!isRunning() || activePlayer == null) return;
        if (pendingRotation != null) return; // don't start a second rotation while one is pending

        // Cancel any ongoing countdown (e.g. from startGame or a previous switch)
        cancelCountdown();

        // Capture current player state
        Player oldActive = activePlayer;
        PlayerData snapshot = PlayerData.capture(oldActive);

        // Save mount/passenger/shoulder references before teleporting old player
        Entity oldVehicle = oldActive.getVehicle();
        List<Entity> oldPassengers = new ArrayList<>(oldActive.getPassengers());
        Entity oldShoulderLeft = oldActive.getShoulderEntityLeft();
        Entity oldShoulderRight = oldActive.getShoulderEntityRight();

        // Move old active to end of waiting (loop) or unassign (no loop)
        if (config.isLoop()) {
            oldActive.setGameMode(GameMode.ADVENTURE);
            oldActive.setFallDistance(0);
            lobbyManager.teleportToLobby(oldActive);
            waitingPlayers.add(oldActive);
            assignTeam(oldActive, yellowTeam);
        } else {
            oldActive.setGameMode(GameMode.SPECTATOR);
            clearPlayerDisplay(oldActive);
            greenTeam.removePlayer(oldActive);
        }

        updateWaitingPrefixes();

        // Skip offline players at the front, or attempt bring-back for external lobby
        while (!waitingPlayers.isEmpty() && offlineWaiting.contains(waitingPlayers.getFirst().getUniqueId())) {
            if (lobbyMessenger != null && config.isExternalLobby()) {
                // Attempt to bring this player back from the external lobby
                UUID uuid = waitingPlayers.getFirst().getUniqueId();
                String name = waitingPlayers.getFirst().getName();

                PendingRotation pr = new PendingRotation(uuid, snapshot, oldActive,
                    oldVehicle, oldPassengers, oldShoulderLeft, oldShoulderRight);
                pendingRotation = pr;

                CompletableFuture<Void> future = lobbyMessenger.bringBack(uuid, name);

                // Handle immediate failures synchronously so pendingRotation doesn't leak
                if (future.isCompletedExceptionally()) {
                    pendingRotation = null;
                    waitingPlayers.removeIf(p -> p.getUniqueId().equals(uuid));
                    offlineWaiting.remove(uuid);
                    updateWaitingPrefixes();
                    skipOfflinePlayers();
                    if (waitingPlayers.isEmpty() && activePlayer == null) {
                        endGame(false);
                    }
                    return;
                }

                future.whenCompleteAsync((v, ex) -> {
                    // Only handle if we still own this pendingRotation instance
                    // (prevents races with endGame/removePlayer/disable)
                    if (pendingRotation != pr) return;

                    pendingRotation = null;

                    if (ex instanceof CancellationException) {
                        // cancelled externally (e.g. game ended) — nothing more to do
                        return;
                    }

                    if (ex != null || pr.cancelled) {
                        // 超时、立即失败或已取消 — 跳过该玩家
                        waitingPlayers.removeIf(p -> p.getUniqueId().equals(uuid));
                        offlineWaiting.remove(uuid);
                        updateWaitingPrefixes();
                        skipOfflinePlayers();
                        if (waitingPlayers.isEmpty() && activePlayer == null) {
                            endGame(false);
                        }
                    } else {
                        completeRotationAfterBringback(uuid);
                    }
                }, task -> Bukkit.getGlobalRegionScheduler().run(plugin, _ -> task.run()));
                return;
            }
            // Normal mode: skip offline players
            Player skipped = waitingPlayers.removeFirst();
            offlineWaiting.remove(skipped.getUniqueId());
            updateWaitingPrefixes();
        }

        // Check if there's a next player
        if (waitingPlayers.isEmpty()) {
            activePlayer = null;
            endGame(false);
            return;
        }

        // Pop next player and complete rotation synchronously
        Player next = waitingPlayers.removeFirst();
        activateNextPlayer(snapshot, oldActive, next,
            oldVehicle, oldPassengers, oldShoulderLeft, oldShoulderRight);
    }

    /**
     * Complete a deferred player rotation after the target has arrived from the
     * external lobby. Called from the LobbyMessenger bring-back callback.
     */
    public void completeRotationAfterBringback(UUID arrivedUuid) {
        if (pendingRotation == null || pendingRotation.cancelled) return;
        if (!pendingRotation.playerUuid.equals(arrivedUuid)) return;

        PendingRotation pr = pendingRotation;
        pendingRotation = null;

        // Find the player in the waiting list (handlePlayerJoin restored the reference)
        Player next = null;
        for (int i = 0; i < waitingPlayers.size(); i++) {
            if (waitingPlayers.get(i).getUniqueId().equals(arrivedUuid)) {
                next = waitingPlayers.remove(i);
                break;
            }
        }
        if (next == null) {
            // Player disappeared — try next
            skipOfflinePlayers();
            if (waitingPlayers.isEmpty() && activePlayer == null) {
                endGame(false);
            }
            return;
        }

        activateNextPlayer(pr.snapshot, pr.oldActive, next,
            pr.oldVehicle, pr.oldPassengers, pr.oldShoulderLeft, pr.oldShoulderRight);
    }

    /**
     * Called by LobbyMessenger when a bring-back times out.
     * Skips the timed-out player and tries the next in queue.
     */
    public void onBringBackTimeout(UUID uuid) {
        if (pendingRotation == null || !pendingRotation.playerUuid.equals(uuid)) return;

        PendingRotation pr = pendingRotation;
        pendingRotation = null;

        // Remove the timed-out player from queue
        waitingPlayers.removeIf(p -> p.getUniqueId().equals(uuid));
        offlineWaiting.remove(uuid);
        updateWaitingPrefixes();
        plugin.getLogger().warning("Bring-back timed out for player " + uuid + ", skipping.");

        // Skip any remaining offline players
        skipOfflinePlayers();

        if (waitingPlayers.isEmpty()) {
            if (activePlayer == null) {
                endGame(false);
            }
            return;
        }

        // Activate the next available player
        Player next = waitingPlayers.removeFirst();
        activateNextPlayer(pr.snapshot, pr.oldActive, next,
            pr.oldVehicle, pr.oldPassengers, pr.oldShoulderLeft, pr.oldShoulderRight);
    }

    /**
     * Activate a player as the new active player, applying the snapshot and all
     * entity transfers. Used by both synchronous switching and async bring-back.
     */
    private void activateNextPlayer(PlayerData snapshot, Player oldActive, Player next,
                                    Entity oldVehicle, List<Entity> oldPassengers,
                                    Entity oldShoulderLeft, Entity oldShoulderRight) {
        activePlayer = next;

        // Apply snapshot
        snapshot.apply(next);
        redirectMobAggro(oldActive, next);
        transferPets(oldActive, next);

        // Transfer mount/passenger/shoulder to the new player
        if (oldVehicle != null) {
            oldVehicle.addPassenger(next);
        }
        for (Entity passenger : oldPassengers) {
            next.addPassenger(passenger);
        }
        if (oldShoulderLeft != null) {
            next.setShoulderEntityLeft(oldShoulderLeft);
        }
        if (oldShoulderRight != null) {
            next.setShoulderEntityRight(oldShoulderRight);
        }

        next.setGameMode(GameMode.SURVIVAL);
        next.setHasSeenWinScreen(true);
        assignTeam(next, greenTeam);
        applyActiveDisplay(next);

        updateWaitingPrefixes();
        remainingTicks = config.getTurnDuration();
        lastReminderCheck = -1; // reset reminder tracking for the new turn
        updateBossBar();

        // Freeze + 10s countdown before the new player can move (skipped when freeze is disabled)
        if (config.isFreeze()) {
            startCountdown(10, next, () -> {
                Title goTitle = Title.title(
                    plugin.getTranslator().translate("game.go.title"),
                    Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(300))
                );
                next.showTitle(goTitle);
            });
        }
    }

    /** Skip all offline players at the front of the waiting queue. */
    private void skipOfflinePlayers() {
        while (!waitingPlayers.isEmpty() && offlineWaiting.contains(waitingPlayers.getFirst().getUniqueId())) {
            Player skipped = waitingPlayers.removeFirst();
            offlineWaiting.remove(skipped.getUniqueId());
            updateWaitingPrefixes();
        }
    }

    public void endGame(boolean wonByPortal) {
        if (gameState != GameState.RUNNING) return;
        gameState = GameState.IDLE;

        cancelCountdown();
        stopTimer();

        // Cancel any pending bring-back rotation
        if (pendingRotation != null) {
            pendingRotation.cancelled = true;
            pendingRotation = null;
        }
        if (lobbyMessenger != null) {
            lobbyMessenger.cancelAllPending();
        }

        try {
            Bukkit.getServerTickManager().setFrozen(true);
        } catch (Exception ignored) {
        }

        Location loc = activePlayer != null ? activePlayer.getLocation() : null;

        // Clear waiting players — become unassigned
        for (Player p : waitingPlayers) {
            p.setGameMode(GameMode.SPECTATOR);
            clearPlayerDisplay(p);
            yellowTeam.removePlayer(p);
        }
        waitingPlayers.clear();
        offlineWaiting.clear();

        // Teleport all online players to the active player's location
        if (loc != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.teleport(loc);
            }
        }

        activePlayer = null;

        // Show title
        Component titleText = wonByPortal
            ? plugin.getTranslator().translate("game.end.cleared")
            : plugin.getTranslator().translate("game.end.over");
        long elapsed = System.currentTimeMillis() - gameStartTime;
        long totalSec = elapsed / 1000;
        Component subtitleText = plugin.getTranslator().translate(
            "game.end.subtitle",
            String.format("%d:%02d", totalSec / 60, totalSec % 60)
        );
        Title title = Title.title(titleText, subtitleText,
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000)));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
        }

        // Clean up boss bar
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

    }

    public void winGame(Player player, Location portalLoc) {
        if (!isRunning() || !player.equals(activePlayer)) return;
        if (portalLoc != null && portalLoc.getWorld() != null) {
            for (int x = -5; x <= 5; x++) {
                for (int y = -1; y <= 2; y++) {
                    for (int z = -5; z <= 5; z++) {
                        Location check = portalLoc.clone().add(x, y, z);
                        if (check.getBlock().getType() == Material.END_PORTAL) {
                            check.getBlock().setType(Material.AIR);
                        }
                    }
                }
            }
        }
        endGame(true);
    }

    // --- Entity bindings inheritance ---

    /** Transfer mob aggro, leash, and thrown ender pearls from {@code from} to {@code to}.
     * Scans every loaded world except the lobby.
     * Pet ownership is handled separately by {@link #transferPets} with deferred chunk-load support. */
    private void redirectMobAggro(Player from, Player to) {
        World lobbyWorld = lobbyManager.getLobbyWorld();
        for (World world : Bukkit.getWorlds()) {
            if (world.equals(lobbyWorld)) {
                continue;
            }
            for (LivingEntity living : world.getLivingEntities()) {
                if (living instanceof Mob mob && from.equals(mob.getTarget())) {
                    mob.setTarget(to);
                }
                if (living.isLeashed() && from.equals(living.getLeashHolder())) {
                    living.setLeashHolder(to);
                }
            }
        }
        for (EnderPearl pearl : from.getEnderPearls()) {
            pearl.setShooter(to);
        }
    }

    // --- Pet ownership reverse index ---

    /** Record a tamed entity in the reverse index. Called from {@code EntityTameEvent}. */
    public void onEntityTame(UUID playerUUID, UUID entityUUID) {
        // Remove from any previous owner to handle re-taming
        petOwnershipIndex.values().forEach(set -> set.remove(entityUUID));
        pendingPetTransfers.remove(entityUUID);
        petOwnershipIndex.computeIfAbsent(playerUUID, _ -> new HashSet<>()).add(entityUUID);
    }

    /** Remove a dead entity from the index and pending transfers. Called from {@code EntityDeathEvent}. */
    public void onEntityDeath(UUID entityUUID) {
        petOwnershipIndex.values().forEach(set -> set.remove(entityUUID));
        pendingPetTransfers.remove(entityUUID);
    }

    /** Apply pending pet transfers when a chunk loads. Called from {@code EntitiesLoadEvent}. */
    public void onEntitiesLoad(Collection<Entity> entities) {
        for (Entity entity : entities) {
            UUID petUUID = entity.getUniqueId();
            UUID newOwnerUUID = pendingPetTransfers.remove(petUUID);
            if (newOwnerUUID != null && entity instanceof Tameable tameable) {
                Player newOwner = Bukkit.getPlayer(newOwnerUUID);
                if (newOwner != null) {
                    tameable.setOwner(newOwner);
                }
            }
        }
    }

    /** Transfer all pet ownership from one player to another.
     * Loaded pets are setOwner'd immediately; unloaded pets are deferred until chunk load. */
    private void transferPets(Player from, Player to) {
        UUID fromUUID = from.getUniqueId();
        UUID toUUID = to.getUniqueId();

        // Start from the reverse-index set
        Set<UUID> allPets = new HashSet<>(petOwnershipIndex.getOrDefault(fromUUID, Set.of()));

        // Scan loaded worlds to catch stragglers and apply transfers immediately
        World lobbyWorld = lobbyManager.getLobbyWorld();
        for (World world : Bukkit.getWorlds()) {
            if (world.equals(lobbyWorld)) continue;
            for (LivingEntity living : world.getLivingEntities()) {
                if (living instanceof Tameable tameable) {
                    AnimalTamer owner = tameable.getOwner();
                    if (owner != null && fromUUID.equals(owner.getUniqueId())) {
                        allPets.add(living.getUniqueId());
                        tameable.setOwner(to);
                    }
                }
            }
        }

        // For index entries not caught by the scan (unloaded), defer
        for (UUID petUUID : allPets) {
            Entity entity = Bukkit.getEntity(petUUID);
            if (entity instanceof Tameable tameable && !to.equals(tameable.getOwner())) {
                // Race: loaded between scan and now — apply
                tameable.setOwner(to);
            } else if (entity == null) {
                // Still unloaded — defer until chunk load
                pendingPetTransfers.put(petUUID, toUUID);
            }
            // else: already handled by scan
        }

        // Transfer index to new owner
        if (!allPets.isEmpty()) {
            petOwnershipIndex.put(toUUID, allPets);
        }
        petOwnershipIndex.remove(fromUUID);
    }

    // --- Timer ---

    private void startTimer() {
        stopTimer();
        timerTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin,
                _ -> tick(), 1L, 1L);
    }

    private void stopTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    private void tick() {
        if (gameState != GameState.RUNNING) return;
        if (Bukkit.getServerTickManager().isFrozen()) return;
        if (countdownActive) return;

        remainingTicks--;

        if (remainingTicks % 20 == 0) {
            updateBossBar();
            checkWaitingReminders();
        }

        if (remainingTicks <= 0) {
            // If a bring-back is pending, don't start another switch
            if (pendingRotation != null) return;
            switchToNextPlayer();
        }
    }

    /**
     * Send waiting reminders to the next player in queue at specific thresholds
     * (100%, 60%, 20%, 10s remaining of the current turn).
     */
    private void checkWaitingReminders() {
        if (waitingPlayers.isEmpty()) return;

        int currentSec = remainingTicks / 20;
        if (currentSec == lastReminderCheck) return;
        lastReminderCheck = currentSec;

        int totalSec = config.getTurnDuration() / 20;
        int[] checkpoints;
        if (totalSec > 10) {
            checkpoints = new int[]{totalSec, (int) (totalSec * 0.6), (int) (totalSec * 0.2), 10};
        } else {
            checkpoints = new int[]{totalSec, 10};
        }

        for (int cp : checkpoints) {
            if (currentSec == cp) {
                sendReminderToNext(currentSec);
                break;
            }
        }
    }

    /**
     * Send a chat reminder to the first player in the waiting queue.
     * If they are on the external lobby, send via BungeeCord Message subchannel.
     */
    private void sendReminderToNext(int secondsRemaining) {
        Player next = waitingPlayers.getFirst();
        Component message = plugin.getTranslator().translate("game.reminder.next",
            String.valueOf(secondsRemaining));

        if (next.isOnline()) {
            next.sendMessage(message);
        } else if (lobbyMessenger != null && config.isExternalLobby()) {
            lobbyMessenger.sendMessage(next, message);
        }
        // If fully offline (not on lobby server), the message is lost — acceptable.
    }

    // --- Player removal (disconnect) ---

    public void removePlayer(Player player) {
        cancelCountdown();
        removePlayerFromBossBar(player);

        // Cancel pending bring-back for this player.
        // Handles both cases: the bring-back target disconnected, *or* the old active player
        // (whose snapshot is held in the rotation) disconnected during the 30s wait.
        if (pendingRotation != null) {
            boolean matchesTarget = pendingRotation.playerUuid.equals(player.getUniqueId());
            boolean matchesOldActive = pendingRotation.oldActive != null
                && pendingRotation.oldActive.equals(player);
            if (matchesTarget || matchesOldActive) {
                UUID targetUuid = pendingRotation.playerUuid;
                pendingRotation.cancelled = true;
                pendingRotation = null;
                // Cancel the messenger's pending future for the *target* (not the disconnecting player)
                if (lobbyMessenger != null) {
                    lobbyMessenger.cancelBringBack(targetUuid);
                }
            }
        }
        if (lobbyMessenger != null) {
            lobbyMessenger.cancelBringBack(player.getUniqueId());
        }

        if (isActivePlayer(player)) {
            // Active player disconnected — same as /rr next logic
            PlayerData snapshot = PlayerData.capture(player);

            // Save mount/passenger references before disconnect
            Entity vehicle = player.getVehicle();
            List<Entity> passengers = new ArrayList<>(player.getPassengers());
            // No shoulder entity save — the player is gone

            activePlayer = null;
            skipOfflinePlayers();
            if (!waitingPlayers.isEmpty()) {
                Player next = waitingPlayers.removeFirst();
                activateNextPlayer(snapshot, player, next,
                    vehicle, passengers, null, null);
            } else {
                endGame(false);
            }
        } else if (isWaiting(player)) {
            if (isRunning()) {
                // During game: preserve queue position, mark as offline
                offlineWaiting.add(player.getUniqueId());
                yellowTeam.removePlayer(player);
                clearPlayerDisplay(player);
            } else {
                // Before game: remove from queue entirely
                waitingPlayers.remove(player);
                updateWaitingPrefixes();
            }
        }
        // Unassigned players: nothing to clean up
        greenTeam.removePlayer(player);
        yellowTeam.removePlayer(player);
    }

    // --- Cleanup ---

    public void disable() {
        if (pendingRotation != null) {
            pendingRotation.cancelled = true;
            pendingRotation = null;
        }
        cancelCountdown();
        stopTimer();
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        petOwnershipIndex.clear();
        pendingPetTransfers.clear();
    }
}
