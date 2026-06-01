package top.syshub.relayRace;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GameManager {

    private final RelayRace plugin;
    private final LobbyManager lobbyManager;
    private GameState gameState = GameState.IDLE;

    private int turnDuration; // in ticks
    private int remainingTicks;

    private Player activePlayer;
    private final List<Player> waitingPlayers = new ArrayList<>();
    private final Set<Player> spectators = new LinkedHashSet<>();
    private boolean sorted;

    private BossBar bossBar;
    private ScheduledTask timerTask;

    private Team greenTeam;
    private Team yellowTeam;

    private long gameStartTime;
    private boolean debug;
    private boolean loop;

    public GameManager(RelayRace plugin, LobbyManager lobbyManager) {
        this.plugin = plugin;
        this.lobbyManager = lobbyManager;
        setupTeams();
    }

    // --- Config ---

    public void loadConfig() {
        plugin.getConfig().addDefault("time", 300);
        plugin.getConfig().addDefault("debug", false);
        plugin.getConfig().addDefault("loop", true);
        plugin.getConfig().options().copyDefaults(true);
        saveConfigAsync();
        turnDuration = plugin.getConfig().getInt("time") * 20;
        debug = plugin.getConfig().getBoolean("debug");
        loop = plugin.getConfig().getBoolean("loop");
    }

    public int getPlaytimeSeconds() {
        return turnDuration / 20;
    }

    public void setPlaytimeSeconds(int seconds) {
        turnDuration = seconds * 20;
        if (remainingTicks > turnDuration) {
            remainingTicks = turnDuration;
        }
        plugin.getConfig().set("time", seconds);
        saveConfigAsync();
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

    public boolean isSpectator(Player player) {
        return spectators.contains(player);
    }

    public boolean isInGame(Player player) {
        return isActivePlayer(player) || isWaiting(player) || isSpectator(player);
    }

    public boolean isSorted() {
        return sorted;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        plugin.getConfig().set("debug", debug);
        saveConfigAsync();
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
        plugin.getConfig().set("loop", loop);
        saveConfigAsync();
    }

    private void saveConfigAsync() {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> plugin.saveConfig());
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

    // --- Boss Bar ---

    private void createBossBar() {
        NamespacedKey key = new NamespacedKey(plugin, "remaining_time");
        bossBar = Bukkit.createBossBar(key, formattedTimeString(), BarColor.GREEN, BarStyle.SEGMENTED_10);
        bossBar.setVisible(true);
    }

    private void updateBossBar() {
        if (bossBar == null) return;
        bossBar.setTitle(formattedTimeString());
        float progress = turnDuration > 0 ? (float) remainingTicks / turnDuration : 0;
        bossBar.setProgress(Math.max(0, Math.min(1, progress)));
        if (progress < 0.25) {
            bossBar.setColor(BarColor.RED);
        } else if (progress < 0.5) {
            bossBar.setColor(BarColor.YELLOW);
        } else {
            bossBar.setColor(BarColor.GREEN);
        }
    }

    private String formattedTimeString() {
        int seconds = remainingTicks / 20;
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("Remaining: %d:%02d", min, sec);
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

    public void addToWaiting(Player player) {
        removeFromAllGroups(player);
        waitingPlayers.add(player);
        assignTeam(player, yellowTeam);
        addPlayerToBossBar(player);
        sorted = false;
    }

    public void removeFromWaiting(Player player) {
        removeFromAllGroups(player);
        if (sorted) {
            updateWaitingPrefixes();
        }
    }

    public void addSpectator(Player player) {
        removeFromAllGroups(player);
        addSpectatorInternal(player);
        lobbyManager.teleportToLobby(player);
        if (isRunning()) {
            player.setGameMode(GameMode.SPECTATOR);
        } else {
            player.setGameMode(GameMode.ADVENTURE);
        }
        addPlayerToBossBar(player);
    }

    private void addSpectatorInternal(Player player) {
        spectators.add(player);
        greenTeam.removePlayer(player);
        yellowTeam.removePlayer(player);
    }

    private void removeFromAllGroups(Player player) {
        waitingPlayers.remove(player);
        spectators.remove(player);
        greenTeam.removePlayer(player);
        yellowTeam.removePlayer(player);
        if (activePlayer != null && activePlayer.equals(player)) {
            activePlayer = null;
        }
    }

    // --- Sorting ---

    public void sortWaiting() {
        Collections.shuffle(waitingPlayers);
        sorted = true;
        updateWaitingPrefixes();
    }

    private void updateWaitingPrefixes() {
        for (int i = 0; i < waitingPlayers.size(); i++) {
            Player p = waitingPlayers.get(i);
            Component prefix = Component.text("[" + (i + 1) + "] " + p.getName(), NamedTextColor.YELLOW);
            p.playerListName(prefix);
            p.displayName(prefix);
        }
    }

    // --- Game lifecycle ---

    public boolean startGame() {
        if (isRunning()) {
            return false;
        }
        if (waitingPlayers.isEmpty()) {
            return false;
        }
        if (!sorted) {
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
        activePlayer.teleport(worldSpawn);
        activePlayer.setRespawnLocation(worldSpawn, true);

        // Reset to initial state
        PlayerData.reset(activePlayer);
        activePlayer.setHasSeenWinScreen(true);
        double maxHealth = activePlayer.getAttribute(Attribute.MAX_HEALTH) != null
            ? activePlayer.getAttribute(Attribute.MAX_HEALTH).getBaseValue() : 20.0;
        activePlayer.setHealth(maxHealth);
        activePlayer.setFoodLevel(20);
        activePlayer.setSaturation(5);

        Component greenName = Component.text(activePlayer.getName(), NamedTextColor.GREEN);
        activePlayer.playerListName(greenName);
        activePlayer.displayName(greenName);

        // Waiting players stay in lobby, adventure mode
        for (Player p : waitingPlayers) {
            p.setGameMode(GameMode.ADVENTURE);
            lobbyManager.teleportToLobby(p);
        }
        updateWaitingPrefixes();

        // Spectators -> spectator mode
        for (Player p : spectators) {
            p.setGameMode(GameMode.SPECTATOR);
        }

        // Boss bar
        remainingTicks = turnDuration;
        createBossBar();
        updateBossBar();
        for (Player p : Bukkit.getOnlinePlayers()) {
            addPlayerToBossBar(p);
        }

        startTimer();
        return true;
    }

    public void switchToNextPlayer() {
        if (!isRunning() || activePlayer == null) return;

        // Capture current player state
        PlayerData snapshot = PlayerData.capture(activePlayer);

        // Move old active to end of waiting or spectator
        if (loop) {
            activePlayer.setGameMode(GameMode.ADVENTURE);
            activePlayer.setFallDistance(0);
            lobbyManager.teleportToLobby(activePlayer);
            waitingPlayers.add(activePlayer);
            assignTeam(activePlayer, yellowTeam);
            if (sorted) {
                int i = waitingPlayers.size();
                Component prefix = Component.text("[" + i + "] " + activePlayer.getName(), NamedTextColor.YELLOW);
                activePlayer.playerListName(prefix);
                activePlayer.displayName(prefix);
            }
        } else {
            activePlayer.setGameMode(GameMode.SPECTATOR);
            activePlayer.playerListName(null);
            activePlayer.displayName(null);
            greenTeam.removePlayer(activePlayer);
            spectators.add(activePlayer);
        }

        // Check if there's a next player
        if (waitingPlayers.isEmpty()) {
            activePlayer = null;
            endGame(false);
            return;
        }

        // Pop next player
        Player next = waitingPlayers.removeFirst();
        activePlayer = next;

        // Apply snapshot
        snapshot.apply(next);
        next.setGameMode(GameMode.SURVIVAL);
        next.setHasSeenWinScreen(true);
        assignTeam(next, greenTeam);
        Component greenName = Component.text(next.getName(), NamedTextColor.GREEN);
        next.playerListName(greenName);
        next.displayName(greenName);

        updateWaitingPrefixes();
        remainingTicks = turnDuration;
        updateBossBar();
    }

    public void endGame(boolean wonByPortal) {
        if (gameState != GameState.RUNNING) return;
        gameState = GameState.IDLE;

        stopTimer();

        try {
            Bukkit.getServerTickManager().setFrozen(true);
        } catch (Exception ignored) {
        }

        Location loc = activePlayer != null ? activePlayer.getLocation() : null;

        // Move waiting players to spectator
        for (Player p : waitingPlayers) {
            p.setGameMode(GameMode.SPECTATOR);
            p.playerListName(null);
            p.displayName(null);
            yellowTeam.removePlayer(p);
            spectators.add(p);
        }
        waitingPlayers.clear();

        // Teleport all spectators to the active player
        if (loc != null) {
            for (Player p : spectators) {
                p.teleport(loc);
            }
        }

        activePlayer = null;

        // Show title
        Component titleText;
        if (wonByPortal) {
            titleText = Component.text("Game Cleared!", NamedTextColor.GOLD);
        } else {
            titleText = Component.text("Game Over", NamedTextColor.RED);
        }
        long elapsed = System.currentTimeMillis() - gameStartTime;
        long totalSec = elapsed / 1000;
        Component subtitleText = Component.text(
            String.format("Total time: %d:%02d", totalSec / 60, totalSec % 60),
            NamedTextColor.WHITE
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

        sorted = false;
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

    // --- Timer ---

    private void startTimer() {
        stopTimer();
        timerTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin,
            task -> tick(), 1L, 1L);
    }

    private void stopTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    private void tick() {
        if (gameState != GameState.RUNNING) return;

        remainingTicks--;

        if (remainingTicks % 20 == 0) {
            updateBossBar();
        }

        if (remainingTicks <= 0) {
            switchToNextPlayer();
        }
    }

    // --- Player removal (disconnect) ---

    public void removePlayer(Player player) {
        removePlayerFromBossBar(player);
        if (isActivePlayer(player)) {
            PlayerData snapshot = PlayerData.capture(player);
            activePlayer = null;
            if (!waitingPlayers.isEmpty()) {
                activePlayer = waitingPlayers.removeFirst();
                snapshot.apply(activePlayer);
                activePlayer.setGameMode(GameMode.SURVIVAL);
                activePlayer.setHasSeenWinScreen(true);
                assignTeam(activePlayer, greenTeam);
                Component greenName = Component.text(activePlayer.getName(), NamedTextColor.GREEN);
                activePlayer.playerListName(greenName);
                activePlayer.displayName(greenName);
                updateWaitingPrefixes();
                remainingTicks = turnDuration;
                updateBossBar();
            } else {
                endGame(false);
            }
        } else if (isWaiting(player)) {
            waitingPlayers.remove(player);
            if (sorted) {
                updateWaitingPrefixes();
            }
        } else if (isSpectator(player)) {
            spectators.remove(player);
        }
        greenTeam.removePlayer(player);
        yellowTeam.removePlayer(player);
    }

    // --- Cleanup ---

    public void disable() {
        stopTimer();
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }
}
