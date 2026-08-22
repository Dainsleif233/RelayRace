package top.syshub.relayrace.common;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import top.syshub.relayrace.common.api.BossBarHandle;
import top.syshub.relayrace.common.api.CancellableTask;
import top.syshub.relayrace.common.api.Platform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class GameManager {

    private final RelayRacePlugin plugin;
    private final LobbyManager lobbyManager;
    private final RelayRaceConfig config;
    private final Platform platform;
    private GameState gameState = GameState.IDLE;

    private int remainingTicks;

    private Player activePlayer;
    private final List<Player> waitingPlayers = new ArrayList<>();
    private final Set<UUID> offlineWaiting = new HashSet<>();

    private BossBarHandle bossBar;
    private CancellableTask timerTask;
    private CancellableTask countdownTask;

    private Team playingTeam;
    private Team waitingTeam;
    private boolean countdownActive = false;

    private long gameStartTime;

    private LobbyMessenger lobbyMessenger;

    private PendingRotation pendingRotation;

    private int lastReminderCheck = -1;

    private final Map<UUID, Set<UUID>> petOwnershipIndex = new HashMap<>();
    private final Map<UUID, UUID> pendingPetTransfers = new HashMap<>();

    private final MilestoneManager milestoneManager;

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

    public GameManager(RelayRacePlugin plugin, LobbyManager lobbyManager,
                       RelayRaceConfig config, Platform platform) {
        this.plugin = plugin;
        this.lobbyManager = lobbyManager;
        this.config = config;
        this.platform = platform;
        this.milestoneManager = new MilestoneManager(this, platform, plugin);
        setupTeams();
    }

    public MilestoneManager getMilestoneManager() {
        return milestoneManager;
    }

    public Translator getTranslator() {
        return plugin.getTranslator();
    }

    public void setLobbyMessenger(LobbyMessenger lobbyMessenger) {
        this.lobbyMessenger = lobbyMessenger;
    }

    public Player getActivePlayer() {
        return activePlayer;
    }

    // --- Config convenience ---

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

    public boolean isISpec(Player player) {
        return !isActivePlayer(player) && !isWaiting(player);
    }

    // --- Teams ---

    private void setupTeams() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        Scoreboard scoreboard = manager.getMainScoreboard();
        playingTeam = scoreboard.getTeam("RR_Playing");
        if (playingTeam == null) {
            playingTeam = scoreboard.registerNewTeam("RR_Playing");
        }
        playingTeam.setColor(ChatColor.GREEN);
        playingTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        waitingTeam = scoreboard.getTeam("RR_Waiting");
        if (waitingTeam == null) {
            waitingTeam = scoreboard.registerNewTeam("RR_Waiting");
        }
        waitingTeam.setColor(ChatColor.YELLOW);
        waitingTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
    }

    private void assignTeam(Player player, Team team) {
        playingTeam.removeEntry(player.getName());
        waitingTeam.removeEntry(player.getName());
        team.addEntry(player.getName());
    }

    // --- Display ---

    private void applyActiveDisplay(Player player) {
        platform.ui().setPlayerListName(player, "<green><bold>" + player.getName());
        platform.ui().setPlayerDisplayName(player, "<green><bold>" + player.getName());
    }

    private void updateWaitingPrefixes() {
        for (int i = 0; i < waitingPlayers.size(); i++) {
            Player p = waitingPlayers.get(i);
            if (i == 0) {
                platform.ui().setPlayerListName(p, "<yellow><bold>[1] " + p.getName());
                platform.ui().setPlayerDisplayName(p, "<yellow><bold>[1] " + p.getName());
            } else {
                platform.ui().setPlayerListName(p, "<yellow>[" + (i + 1) + "] " + p.getName());
                platform.ui().setPlayerDisplayName(p, "<yellow>[" + (i + 1) + "] " + p.getName());
            }
        }
    }

    private void clearPlayerDisplay(Player player) {
        platform.ui().clearDisplayName(player);
    }

    // --- Boss Bar ---

    private void createBossBar() {
        bossBar = platform.ui().createBossBar(plugin, formattedTimeString(), "GREEN");
        if (bossBar != null) {
            bossBar.setVisible(true);
        }
    }

    private void updateBossBar() {
        if (bossBar == null) return;
        bossBar.setTitle(formattedTimeString());
        double progress = config.getTurnDuration() > 0
            ? (double) remainingTicks / config.getTurnDuration() : 0;
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        if (progress < 0.25) {
            bossBar.setColor("RED");
        } else if (progress < 0.5) {
            bossBar.setColor("YELLOW");
        } else {
            bossBar.setColor("GREEN");
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

        return plugin.getTranslator().plain("game.bossbar.title",
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

    public void handlePlayerJoin(Player player) {
        if (offlineWaiting.remove(player.getUniqueId())) {
            for (int i = 0; i < waitingPlayers.size(); i++) {
                if (waitingPlayers.get(i).getUniqueId().equals(player.getUniqueId())) {
                    waitingPlayers.set(i, player);
                    break;
                }
            }
            assignTeam(player, waitingTeam);
            updateWaitingPrefixes();
            addPlayerToBossBar(player);

            boolean isBeingRecalled = pendingRotation != null
                && pendingRotation.playerUuid.equals(player.getUniqueId());
            if (isBeingRecalled) {
                player.setGameMode(GameMode.SURVIVAL);
            } else {
                lobbyManager.teleportToLobby(player);
                player.setGameMode(GameMode.ADVENTURE);
            }

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

    public void addToWaiting(Player player) {
        removeFromAllGroups(player);
        waitingPlayers.add(player);
        assignTeam(player, waitingTeam);
        addPlayerToBossBar(player);

        if (isRunning()) {
            lobbyManager.teleportToLobby(player);
            player.setGameMode(GameMode.ADVENTURE);
        }

        updateWaitingPrefixes();
    }

    public void removeFromWaiting(Player player) {
        waitingPlayers.remove(player);
        waitingTeam.removeEntry(player.getName());
        clearPlayerDisplay(player);

        if (isRunning()) {
            player.setGameMode(GameMode.SPECTATOR);
        }

        updateWaitingPrefixes();
    }

    private void removeFromAllGroups(Player player) {
        waitingPlayers.removeIf(p -> p.getUniqueId().equals(player.getUniqueId()));
        playingTeam.removeEntry(player.getName());
        waitingTeam.removeEntry(player.getName());
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
        platform.tickControl().restore();
    }

    private void startCountdown(int seconds, final Player target, final Runnable onComplete) {
        cancelCountdown();
        platform.tickControl().slowTo(1.0f);

        final int[] remaining = {seconds};
        countdownActive = true;

        countdownTask = platform.scheduler().runAtFixedRate(plugin, () -> {
            if (remaining[0] > 0) {
                platform.ui().sendTitle(target,
                    plugin.getTranslator().format("game.countdown.number", String.valueOf(remaining[0])),
                    "", 0, 18, 2);
                remaining[0]--;
            } else {
                if (countdownTask != null) {
                    countdownTask.cancel();
                }
                countdownTask = null;
                countdownActive = false;
                platform.tickControl().restore();
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
        milestoneManager.onGameStart(activePlayer);

        activePlayer = waitingPlayers.remove(0);
        assignTeam(activePlayer, playingTeam);
        activePlayer.setGameMode(GameMode.SURVIVAL);

        World world = Bukkit.getWorlds().get(0);
        Location worldSpawn = world.getSpawnLocation();
        world.setFullTime(0);
        activePlayer.teleport(worldSpawn);
        platform.setRespawnLocation(activePlayer, worldSpawn);

        PlayerData.reset(activePlayer, platform);
        platform.setHasSeenWinScreen(activePlayer, true);
        plugin.debug("[startGame] activePlayer=" + activePlayer.getName() + " setHasSeenWinScreen called");
        double maxHealth = platform.captureMaxHealth(activePlayer);
        activePlayer.setHealth(maxHealth);
        activePlayer.setFoodLevel(20);
        activePlayer.setSaturation(5);

        applyActiveDisplay(activePlayer);

        for (Player p : waitingPlayers) {
            p.setGameMode(GameMode.ADVENTURE);
            lobbyManager.teleportToLobby(p);
        }
        updateWaitingPrefixes();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isISpec(p)) {
                p.setGameMode(GameMode.SPECTATOR);
            }
        }

        remainingTicks = config.getTurnDuration();
        createBossBar();
        updateBossBar();
        for (Player p : Bukkit.getOnlinePlayers()) {
            addPlayerToBossBar(p);
        }

        if (config.isFreeze()) {
            startCountdown(15, activePlayer, () -> {
                platform.ui().sendTitle(activePlayer,
                    plugin.getTranslator().format("game.go.title"), "", 0, 10, 6);
                startTimer();
            });
        } else {
            startTimer();
        }
        return true;
    }

    public boolean isPendingRotation() {
        return pendingRotation != null;
    }

    @SuppressWarnings("deprecation")
    public void switchToNextPlayer() {
        if (!isRunning() || activePlayer == null) return;
        if (pendingRotation != null) return;

        cancelCountdown();

        Player oldActive = activePlayer;

        collectCraftingDrops(oldActive);

        PlayerData snapshot = PlayerData.capture(oldActive, platform);

        Entity oldVehicle = oldActive.getVehicle();
        List<Entity> oldPassengers = new ArrayList<>(oldActive.getPassengers());
        Entity oldShoulderLeft = oldActive.getShoulderEntityLeft();
        Entity oldShoulderRight = oldActive.getShoulderEntityRight();

        if (config.isLoop()) {
            oldActive.setGameMode(GameMode.ADVENTURE);
            oldActive.setFallDistance(0);
            lobbyManager.teleportToLobby(oldActive);
            waitingPlayers.add(oldActive);
            assignTeam(oldActive, waitingTeam);
        } else {
            oldActive.setGameMode(GameMode.SPECTATOR);
            clearPlayerDisplay(oldActive);
            playingTeam.removeEntry(oldActive.getName());
        }

        updateWaitingPrefixes();

        while (!waitingPlayers.isEmpty() && offlineWaiting.contains(waitingPlayers.get(0).getUniqueId())) {
            if (lobbyMessenger != null && config.isExternalLobby()) {
                UUID uuid = waitingPlayers.get(0).getUniqueId();
                String name = waitingPlayers.get(0).getName();

                final PendingRotation pr = new PendingRotation(uuid, snapshot, oldActive,
                    oldVehicle, oldPassengers, oldShoulderLeft, oldShoulderRight);
                pendingRotation = pr;
                stopTimer();

                final CompletableFuture<Void> future = lobbyMessenger.bringBack(uuid, name);

                if (future.isCompletedExceptionally()) {
                    pendingRotation = null;
                    removeByUuid(uuid);
                    offlineWaiting.remove(uuid);
                    updateWaitingPrefixes();
                    activateOrEndGame(snapshot, oldActive,
                        oldVehicle, oldPassengers, oldShoulderLeft, oldShoulderRight);
                    return;
                }

                future.whenComplete((v, ex) -> {
                    if (pendingRotation != pr) return;

                    if (ex instanceof CancellationException) {
                        pendingRotation = null;
                        return;
                    }

                    if (ex != null || pr.cancelled) {
                        pendingRotation = null;
                        removeByUuid(uuid);
                        offlineWaiting.remove(uuid);
                        updateWaitingPrefixes();
                        activateOrEndGame(pr.snapshot, pr.oldActive,
                            pr.oldVehicle, pr.oldPassengers,
                            pr.oldShoulderLeft, pr.oldShoulderRight);
                    } else {
                        completeRotationAfterBringback(uuid);
                    }
                });
                return;
            }

            Player skipped = waitingPlayers.remove(0);
            offlineWaiting.remove(skipped.getUniqueId());
            updateWaitingPrefixes();
        }

        activateOrEndGame(snapshot, oldActive,
            oldVehicle, oldPassengers, oldShoulderLeft, oldShoulderRight);
    }

    public void completeRotationAfterBringback(UUID arrivedUuid) {
        if (pendingRotation == null || pendingRotation.cancelled) return;
        if (!pendingRotation.playerUuid.equals(arrivedUuid)) return;

        PendingRotation pr = pendingRotation;
        pendingRotation = null;

        Player next = null;
        for (int i = 0; i < waitingPlayers.size(); i++) {
            if (waitingPlayers.get(i).getUniqueId().equals(arrivedUuid)) {
                next = waitingPlayers.remove(i);
                break;
            }
        }
        if (next == null) {
            skipOfflinePlayers();
            if (waitingPlayers.isEmpty() && activePlayer == null) {
                endGame(false);
            }
            return;
        }

        activateNextPlayer(pr.snapshot, pr.oldActive, next,
            pr.oldVehicle, pr.oldPassengers, pr.oldShoulderLeft, pr.oldShoulderRight);
    }

    @SuppressWarnings("deprecation")
    private void activateNextPlayer(PlayerData snapshot, Player oldActive, Player next,
                                    Entity oldVehicle, List<Entity> oldPassengers,
                                    Entity oldShoulderLeft, Entity oldShoulderRight) {
        activePlayer = next;

        snapshot.apply(next, platform);
        redirectMobAggro(oldActive, next);
        transferPets(oldActive, next);

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
        platform.setHasSeenWinScreen(next, true);
        plugin.debug("[activateNextPlayer] next=" + next.getName() + " setHasSeenWinScreen called");
        assignTeam(next, playingTeam);
        applyActiveDisplay(next);
        milestoneManager.onActivePlayerChange(next);

        updateWaitingPrefixes();
        remainingTicks = config.getTurnDuration();
        lastReminderCheck = -1;
        updateBossBar();

        if (config.isFreeze()) {
            startCountdown(10, next, () -> platform.ui().sendTitle(next,
                plugin.getTranslator().format("game.go.title"), "", 0, 10, 6));
        }
        startTimer();
    }

    private void skipOfflinePlayers() {
        while (!waitingPlayers.isEmpty() && offlineWaiting.contains(waitingPlayers.get(0).getUniqueId())) {
            Player skipped = waitingPlayers.remove(0);
            offlineWaiting.remove(skipped.getUniqueId());
            updateWaitingPrefixes();
        }
    }

    private void removeByUuid(UUID uuid) {
        waitingPlayers.removeIf(p -> p.getUniqueId().equals(uuid));
    }

    /**
     * Pick the next online player from the waiting queue and activate them,
     * or end the game if none remain. Extracted so that both the normal
     * switchToNextPlayer path and the bringBack-failure path can reuse it.
     */
    private void activateOrEndGame(PlayerData snapshot, Player oldActive,
                                    Entity oldVehicle, List<Entity> oldPassengers,
                                    Entity oldShoulderLeft, Entity oldShoulderRight) {
        skipOfflinePlayers();

        if (waitingPlayers.isEmpty()) {
            activePlayer = null;
            endGame(false);
            return;
        }

        Player next = waitingPlayers.remove(0);
        activateNextPlayer(snapshot, oldActive, next,
            oldVehicle, oldPassengers, oldShoulderLeft, oldShoulderRight);
    }

    public void endGame(boolean wonByPortal) {
        if (gameState != GameState.RUNNING) return;
        gameState = GameState.IDLE;

        milestoneManager.onGameEnd();

        cancelCountdown();
        stopTimer();

        if (pendingRotation != null) {
            pendingRotation.cancelled = true;
            pendingRotation = null;
        }
        if (lobbyMessenger != null) {
            lobbyMessenger.cancelAllPending();
        }

        try {
            platform.tickControl().setFrozen(true);
        } catch (Exception ignored) {
        }

        Location loc = activePlayer != null ? activePlayer.getLocation() : null;

        for (Player p : waitingPlayers) {
            p.setGameMode(GameMode.SPECTATOR);
            clearPlayerDisplay(p);
            waitingTeam.removeEntry(p.getName());
        }
        waitingPlayers.clear();
        offlineWaiting.clear();

        if (loc != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.teleport(loc);
            }
        }

        activePlayer = null;

        String titleText = wonByPortal
            ? plugin.getTranslator().format("game.end.cleared")
            : plugin.getTranslator().format("game.end.over");
        long elapsed = System.currentTimeMillis() - gameStartTime;
        long totalSec = elapsed / 1000;
        String subtitleText = plugin.getTranslator().format(
            "game.end.subtitle",
            String.format("%d:%02d", totalSec / 60, totalSec % 60));
        for (Player p : Bukkit.getOnlinePlayers()) {
            platform.ui().sendTitle(p, titleText, subtitleText, 10, 60, 20);
        }

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    public void winGame(Player player, Location portalLoc) {
        plugin.debug("[winGame] entered, player=" + (player != null ? player.getName() : "null") + " isRunning=" + isRunning() + " activePlayer=" + (activePlayer != null ? activePlayer.getName() : "null"));
        if (!isRunning() || !player.equals(activePlayer)) return;
        destroyEndPortalBlocks(portalLoc);
        milestoneManager.onClearGame(player);
        endGame(true);
    }

    /**
     * Destroy End portal blocks in an 11×4×11 area around the given location
     * to prevent re-triggering. The classic 1.16.1 platform calls this from
     * its respawn-based win detection before the player respawns back at the
     * portal spot.
     */
    public void destroyEndPortalBlocks(Location portalLoc) {
        if (portalLoc == null || portalLoc.getWorld() == null) return;
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

    private void collectCraftingDrops(Player player) {
        player.getWorld();
        player.closeInventory();
        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
            player.getLocation(), 2.0, 2.0, 2.0);
        for (Entity entity : nearby) {
            if (entity instanceof Item) {
                Item drop = (Item) entity;
                org.bukkit.inventory.ItemStack stack = drop.getItemStack().clone();
                drop.remove();
                player.getInventory().addItem(stack).forEach(
                        (slot, leftover) -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            }
        }
    }

    // --- Entity bindings inheritance ---

    private void redirectMobAggro(Player from, Player to) {
        World lobbyWorld = lobbyManager.getLobbyWorld();
        for (World world : Bukkit.getWorlds()) {
            if (world.equals(lobbyWorld)) {
                continue;
            }
            for (LivingEntity living : world.getLivingEntities()) {
                if (living instanceof Mob && from.equals(((Mob) living).getTarget())) {
                    ((Mob) living).setTarget(to);
                }
                if (living.isLeashed() && from.equals(living.getLeashHolder())) {
                    living.setLeashHolder(to);
                }
            }
        }
        // Transfer thrown ender pearls without relying on newer Player#getEnderPearls().
        for (World world : Bukkit.getWorlds()) {
            if (world.equals(lobbyWorld)) {
                continue;
            }
            for (Entity entity : world.getEntities()) {
                if (entity instanceof EnderPearl) {
                    EnderPearl pearl = (EnderPearl) entity;
                    if (from.equals(pearl.getShooter())) {
                        pearl.setShooter(to);
                    }
                }
            }
        }
    }

    // --- Pet ownership reverse index ---

    public void onEntityTame(UUID playerUUID, UUID entityUUID) {
        for (Set<UUID> set : petOwnershipIndex.values()) {
            set.remove(entityUUID);
        }
        pendingPetTransfers.remove(entityUUID);
        petOwnershipIndex.computeIfAbsent(playerUUID, key -> new HashSet<>()).add(entityUUID);
    }

    public void onEntityDeath(UUID entityUUID) {
        for (Set<UUID> set : petOwnershipIndex.values()) {
            set.remove(entityUUID);
        }
        pendingPetTransfers.remove(entityUUID);
    }

    public void onEntitiesLoad(Collection<Entity> entities) {
        for (Entity entity : entities) {
            UUID petUUID = entity.getUniqueId();
            UUID newOwnerUUID = pendingPetTransfers.remove(petUUID);
            if (newOwnerUUID != null && entity instanceof Tameable) {
                Tameable tameable = (Tameable) entity;
                Player newOwner = Bukkit.getPlayer(newOwnerUUID);
                if (newOwner != null) {
                    tameable.setOwner(newOwner);
                }
            }
        }
    }

    private void transferPets(Player from, Player to) {
        UUID fromUUID = from.getUniqueId();
        UUID toUUID = to.getUniqueId();

        Set<UUID> allPets = new HashSet<>(
                petOwnershipIndex.getOrDefault(fromUUID, Collections.emptySet()));

        World lobbyWorld = lobbyManager.getLobbyWorld();
        for (World world : Bukkit.getWorlds()) {
            if (world.equals(lobbyWorld)) continue;
            for (LivingEntity living : world.getLivingEntities()) {
                if (living instanceof Tameable) {
                    Tameable tameable = (Tameable) living;
                    org.bukkit.entity.AnimalTamer owner = tameable.getOwner();
                    if (owner != null && fromUUID.equals(owner.getUniqueId())) {
                        allPets.add(living.getUniqueId());
                        tameable.setOwner(to);
                    }
                }
            }
        }

        for (UUID petUUID : allPets) {
            Entity entity = Bukkit.getEntity(petUUID);
            if (entity instanceof Tameable) {
                Tameable tameable = (Tameable) entity;
                if (!to.equals(tameable.getOwner())) {
                    tameable.setOwner(to);
                }
            } else if (entity == null) {
                pendingPetTransfers.put(petUUID, toUUID);
            }
        }

        if (!allPets.isEmpty()) {
            petOwnershipIndex.put(toUUID, allPets);
        }
        petOwnershipIndex.remove(fromUUID);
    }

    // --- Timer ---

    private void startTimer() {
        stopTimer();
        timerTask = platform.scheduler().runAtFixedRate(plugin, this::tick, 1L, 1L);
    }

    private void stopTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    private void tick() {
        if (gameState != GameState.RUNNING) return;
        if (platform.tickControl().isFrozen()) return;
        if (countdownActive) return;

        remainingTicks--;

        if (remainingTicks % 20 == 0) {
            updateBossBar();
            checkWaitingReminders();
        }

        if (remainingTicks <= 0) {
            if (pendingRotation != null) return;
            switchToNextPlayer();
        }
    }

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

    private void sendReminderToNext(int secondsRemaining) {
        Player next = waitingPlayers.get(0);
        String message = plugin.getTranslator().format("game.reminder.next",
            String.valueOf(secondsRemaining));

        if (next.isOnline()) {
            platform.ui().sendMessage(next, message);
        } else if (lobbyMessenger != null && config.isExternalLobby()) {
            lobbyMessenger.sendMessage(next, message);
        }
    }

    // --- Player removal ---

    public void removePlayer(Player player) {
        cancelCountdown();
        removePlayerFromBossBar(player);

        if (pendingRotation != null) {
            boolean matchesTarget = pendingRotation.playerUuid.equals(player.getUniqueId());
            boolean matchesOldActive = pendingRotation.oldActive != null
                && pendingRotation.oldActive.equals(player);
            if (matchesTarget || matchesOldActive) {
                UUID targetUuid = pendingRotation.playerUuid;
                pendingRotation.cancelled = true;
                pendingRotation = null;
                if (lobbyMessenger != null) {
                    lobbyMessenger.cancelBringBack(targetUuid);
                }
            }
        }
        if (lobbyMessenger != null) {
            lobbyMessenger.cancelBringBack(player.getUniqueId());
        }

        if (isActivePlayer(player)) {
            PlayerData snapshot = PlayerData.capture(player, platform);

            Entity vehicle = player.getVehicle();
            List<Entity> passengers = new ArrayList<>(player.getPassengers());

            activePlayer = null;
            skipOfflinePlayers();
            if (!waitingPlayers.isEmpty()) {
                Player next = waitingPlayers.remove(0);
                activateNextPlayer(snapshot, player, next,
                    vehicle, passengers, null, null);
            } else {
                endGame(false);
            }
        } else if (isWaiting(player)) {
            if (isRunning()) {
                offlineWaiting.add(player.getUniqueId());
                waitingTeam.removeEntry(player.getName());
                clearPlayerDisplay(player);
            } else {
                waitingPlayers.remove(player);
                updateWaitingPrefixes();
            }
        }

        playingTeam.removeEntry(player.getName());
        waitingTeam.removeEntry(player.getName());
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
        milestoneManager.disable();
    }
}