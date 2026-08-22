package top.syshub.relayrace.common;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import top.syshub.relayrace.common.api.Platform;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the milestone sidebar board for the relay race.
 *
 * <p>The board appears when the game starts and remains visible after the
 * game ends (until the next game resets it). It shows:
 * <ul>
 *   <li>Current active player</li>
 *   <li>Current progress status</li>
 *   <li>One line per achieved milestone (label, player name, elapsed time)</li>
 * </ul>
 *
 * <p>Milestone detection reuses vanilla advancement triggers where possible:
 * <ul>
 *   <li>Bastion remnant entry — {@code minecraft:nether/find_bastion} (光辉岁月)</li>
 *   <li>Nether fortress entry — {@code minecraft:nether/find_fortress} (阴森的要塞)</li>
 *   <li>Stronghold entry — {@code minecraft:story/follow_ender_eye} (隔墙有眼)</li>
 * </ul>
 * Dimension changes (nether / end entry) are detected via
 * {@link org.bukkit.event.player.PlayerChangedWorldEvent}. Ender eye throw
 * (status-only milestone) is detected via {@link org.bukkit.entity.EnderSignal}
 * entity spawn.
 */
public class MilestoneManager {

    // --- Advancement keys (vanilla, 1.16+) ---
    /** 光辉岁月 — entered a bastion remnant. */
    static final String KEY_BASTION    = "minecraft:nether/find_bastion";
    /** 阴森的要塞 — entered a nether fortress. */
    static final String KEY_FORTRESS   = "minecraft:nether/find_fortress";
    /** 隔墙有眼 — entered a stronghold (followed an ender eye). */
    static final String KEY_STRONGHOLD = "minecraft:story/follow_ender_eye";

    private static final String OBJECTIVE_NAME = "rr_milestone";

    public static boolean isBastionKey(String key) {
        return KEY_BASTION.equals(key);
    }

    public static boolean isFortressKey(String key) {
        return KEY_FORTRESS.equals(key);
    }

    public static boolean isStrongholdKey(String key) {
        return KEY_STRONGHOLD.equals(key);
    }

    // Score slots
    private static final int SCORE_PLAYER   = 15;
    private static final int SCORE_STATUS   = 14;
    private static final int SCORE_SEPARATOR = 13;
    private static final int SCORE_FIRST_MILESTONE = 12;

    private final GameManager gameManager;
    private final Platform platform;
    private final RelayRacePlugin plugin;

    // Scoreboard handles
    private Scoreboard scoreboard;
    private Objective objective;

    // Milestone state
    private final List<MilestoneRecord> achieved = new ArrayList<>();
    private int currentStatusTier = -1;
    private String currentStatusKey = null;
    private String lastActivePlayerName = null;
    private long gameStartTime = 0L;
    private boolean active = false;

    // Current scoreboard entry strings (for reset on update)
    private String playerLineEntry = null;
    private String statusLineEntry = null;
    private int nextMilestoneScore = SCORE_FIRST_MILESTONE;

    private static final class MilestoneRecord {
        final MilestoneType type;
        final String playerName;
        final long elapsedSeconds;

        MilestoneRecord(MilestoneType type, String playerName, long elapsedSeconds) {
            this.type = type;
            this.playerName = playerName;
            this.elapsedSeconds = elapsedSeconds;
        }
    }

    public MilestoneManager(GameManager gameManager, Platform platform, RelayRacePlugin plugin) {
        this.gameManager = gameManager;
        this.platform = platform;
        this.plugin = plugin;
    }

    private Translator translator() {
        return plugin.getTranslator();
    }

    // ======================================================================
    //  Game lifecycle
    // ======================================================================

    public void onGameStart(Player activePlayer) {
        resetState();
        active = true;
        gameStartTime = System.currentTimeMillis();
        lastActivePlayerName = activePlayer.getName();
        createBoard();
        achieve(MilestoneType.START_GAME, activePlayer);
    }

    public void onGameEnd() {
        // Board remains visible with the final state.
        active = false;
    }

    public void onActivePlayerChange(Player newActive) {
        if (newActive != null) {
            lastActivePlayerName = newActive.getName();
        }
        updatePlayerLine();
    }

    public void onClearGame(Player player) {
        if (player != null) {
            achieve(MilestoneType.CLEAR_GAME, player);
        } else if (lastActivePlayerName != null) {
            achieveWithName(MilestoneType.CLEAR_GAME, lastActivePlayerName);
        }
    }

    // ======================================================================
    //  Event-driven milestone triggers
    // ======================================================================

    public void onWorldChange(Player player, World newWorld) {
        if (!active) return;
        if (!gameManager.isActivePlayer(player)) return;
        World.Environment env = newWorld.getEnvironment();
        if (env == World.Environment.NETHER) {
            achieve(MilestoneType.ENTER_NETHER, player);
        } else if (env == World.Environment.THE_END) {
            achieve(MilestoneType.ENTER_END, player);
        }
    }

    public void onAdvancement(Player player, MilestoneType type) {
        if (!active) return;
        if (!gameManager.isActivePlayer(player)) return;
        achieve(type, player);
    }

    /**
     * Called when an {@link org.bukkit.entity.EnderSignal} entity spawns near
     * the active player — i.e. the active player successfully threw an ender
     * eye. Only updates the status; does not add a display line.
     */
    public void onEnderEyeUse(Player player) {
        if (!active) return;
        if (player == null) return;
        if (!gameManager.isActivePlayer(player)) return;
        if (currentStatusTier >= MilestoneType.GOING_TO_STRONGHOLD.getTier()) return;
        achieve(MilestoneType.GOING_TO_STRONGHOLD, player);
    }

    public void onEnderDragonDeath() {
        plugin.debug("[milestone] onEnderDragonDeath: active=" + active
            + " activePlayer=" + (gameManager.getActivePlayer() != null
                ? gameManager.getActivePlayer().getName() : "null")
            + " lastActive=" + lastActivePlayerName);
        if (!active) return;
        Player current = gameManager.getActivePlayer();
        String name = current != null ? current.getName() : lastActivePlayerName;
        if (name == null) return;
        achieveWithName(MilestoneType.DEFEAT_DRAGON, name);
    }

    public boolean isActive() {
        return active;
    }

    // ======================================================================
    //  Core achievement logic
    // ======================================================================

    private void achieve(MilestoneType type, Player player) {
        achieveWithName(type, player.getName());
    }

    private void achieveWithName(MilestoneType type, String playerName) {
        // De-duplicate: each milestone type is recorded at most once.
        for (MilestoneRecord r : achieved) {
            if (r.type == type) return;
        }

        long elapsed = gameStartTime > 0
            ? (System.currentTimeMillis() - gameStartTime) / 1000L : 0L;
        achieved.add(new MilestoneRecord(type, playerName, elapsed));

        // Update status (tier-based, same-tier overwrites label)
        if (type.hasStatus()) {
            int tier = type.getTier();
            if (tier > currentStatusTier) {
                currentStatusTier = tier;
                currentStatusKey = type.getStatusKey();
            } else if (tier == currentStatusTier) {
                currentStatusKey = type.getStatusKey();
            }
            updateStatusLine();
        }

        // Add display line on the board
        if (type.hasDisplayLine()) {
            addMilestoneLine(type, playerName, elapsed);
        }

        plugin.debug("[milestone] achieved: " + type + " by " + playerName
            + " at " + formatTime(elapsed));
    }

    // ======================================================================
    //  Scoreboard rendering
    // ======================================================================

    private void createBoard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        scoreboard = manager.getMainScoreboard();

        // Remove any leftover objective from a previous game.
        Objective old = scoreboard.getObjective(OBJECTIVE_NAME);
        if (old != null) {
            old.unregister();
        }

        String title = ChatColor.GOLD.toString() + ChatColor.BOLD
            + translator().plain("milestone.title");
        objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy", title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Separator between header and milestone lines.
        objective.getScore(ChatColor.RESET.toString()).setScore(SCORE_SEPARATOR);

        updatePlayerLine();
        updateStatusLine();

        nextMilestoneScore = SCORE_FIRST_MILESTONE;
    }

    private void updatePlayerLine() {
        if (objective == null) return;
        if (playerLineEntry != null) {
            scoreboard.resetScores(playerLineEntry);
        }
        String label = translator().plain("milestone.current_player");
        String name = lastActivePlayerName != null ? lastActivePlayerName : "-";
        playerLineEntry = ChatColor.AQUA + label + ": " + ChatColor.WHITE + name;
        objective.getScore(playerLineEntry).setScore(SCORE_PLAYER);
    }

    private void updateStatusLine() {
        if (objective == null) return;
        if (statusLineEntry != null) {
            scoreboard.resetScores(statusLineEntry);
        }
        String label = translator().plain("milestone.current_status");
        String status = currentStatusKey != null
            ? translator().plain(currentStatusKey) : "-";
        statusLineEntry = ChatColor.AQUA + label + ": " + ChatColor.WHITE + status;
        objective.getScore(statusLineEntry).setScore(SCORE_STATUS);
    }

    private void addMilestoneLine(MilestoneType type, String playerName, long elapsedSeconds) {
        if (objective == null) return;
        String label = translator().plain(type.getLabelKey());
        String time = formatTime(elapsedSeconds);
        String entry = ChatColor.GREEN + label + " " + ChatColor.YELLOW + playerName
            + " " + ChatColor.GRAY + time;
        objective.getScore(entry).setScore(nextMilestoneScore);
        nextMilestoneScore--;
    }

    private static String formatTime(long seconds) {
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format("%d:%02d", min, sec);
    }

    // ======================================================================
    //  Reset / cleanup
    // ======================================================================

    private void resetState() {
        achieved.clear();
        currentStatusTier = -1;
        currentStatusKey = null;
        lastActivePlayerName = null;
        playerLineEntry = null;
        statusLineEntry = null;
        nextMilestoneScore = SCORE_FIRST_MILESTONE;
    }

    public void disable() {
        if (scoreboard != null) {
            Objective obj = scoreboard.getObjective(OBJECTIVE_NAME);
            if (obj != null) {
                obj.unregister();
            }
        }
        scoreboard = null;
        objective = null;
        active = false;
    }
}
