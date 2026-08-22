package top.syshub.relayrace.classic;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIConfig;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import top.syshub.relayrace.common.GameManager;
import top.syshub.relayrace.common.RelayRacePlugin;
import top.syshub.relayrace.common.api.CommandRegistrar;
import top.syshub.relayrace.common.api.Platform;
import top.syshub.relayrace.common.api.PlayerUi;
import top.syshub.relayrace.common.api.Scheduler;
import top.syshub.relayrace.common.api.TickControl;
import top.syshub.relayrace.common.api.WorldFactory;

import java.lang.reflect.Method;
import java.util.Arrays;

public final class ClassicPlatform implements Platform {

    private static boolean commandApiLoaded;

    private final RelayRacePlugin plugin;
    private final Scheduler scheduler = new ClassicScheduler();
    private final TickControl tickControl = new ClassicTickControl();
    private final WorldFactory worldFactory = new ClassicWorldFactory();
    private final PlayerUi ui = new ClassicPlayerUi();
    private final CommandRegistrar commands = new ClassicCommandHandler();

    public ClassicPlatform(RelayRacePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad(RelayRacePlugin plugin) {
        // The shaded CommandAPI must be bootstrapped exactly once, from the
        // plugin's onLoad(), before any of its commands are registered.
        if (!commandApiLoaded) {
            CommandAPI.onLoad(new CommandAPIConfig());
            commandApiLoaded = true;
        }
    }

    @Override
    public CommandRegistrar commands() {
        return commands;
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public TickControl tickControl() {
        return tickControl;
    }

    @Override
    public WorldFactory worldFactory() {
        return worldFactory;
    }

    @Override
    public PlayerUi ui() {
        return ui;
    }

    @Override
    public void setHasSeenWinScreen(Player player, boolean value) {
        // 1.16.1 has no Player#setHasSeenWinScreen API. The equivalent is the
        // NMS "seenCredits" flag (field ck): with it set to true, the End exit
        // portal sends game-state-change 0.0 (skip the credits screen) so the
        // client immediately requests a respawn, which the common
        // EventListener detects as a credits-respawn to trigger winGame().
        // Paper/Purpur 1.16.1 provides a private setHasSeenCredits(boolean)
        // helper for this.
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Method method = null;
            Class<?> clazz = handle.getClass();
            while (clazz != null) {
                try {
                    method = clazz.getDeclaredMethod("setHasSeenCredits", boolean.class);
                    break;
                } catch (NoSuchMethodException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (method != null) {
                method.setAccessible(true);
                method.invoke(handle, value);
                plugin.debug("[setHasSeenWinScreen] via setHasSeenCredits, player=" + player.getName() + " value=" + value + " handleClass=" + handle.getClass().getName());
            } else {
                plugin.debug("[setHasSeenWinScreen] FAILED to find setHasSeenCredits, player=" + player.getName() + " handleClass=" + handle.getClass().getName());
            }
        } catch (Exception e) {
            plugin.debug("[setHasSeenWinScreen] EXCEPTION for player=" + player.getName() + ": " + e);
        }
    }

    @Override
    public void registerVersionEvents(RelayRacePlugin plugin, GameManager gameManager) {
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onChunkLoad(ChunkLoadEvent event) {
                gameManager.onEntitiesLoad(Arrays.asList(event.getChunk().getEntities()));
            }
        }, plugin);
    }
}
