package top.syshub.relayrace.classic;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIConfig;

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

import java.util.Arrays;

public final class ClassicPlatform implements Platform {

    private static boolean commandApiLoaded;

    private final Scheduler scheduler = new ClassicScheduler();
    private final TickControl tickControl = new ClassicTickControl();
    private final WorldFactory worldFactory = new ClassicWorldFactory();
    private final PlayerUi ui = new ClassicPlayerUi();
    private final CommandRegistrar commands = new ClassicCommandHandler();

    public ClassicPlatform() {}

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
    public void registerVersionEvents(RelayRacePlugin plugin, GameManager gameManager) {
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onChunkLoad(ChunkLoadEvent event) {
                gameManager.onEntitiesLoad(Arrays.asList(event.getChunk().getEntities()));
            }
        }, plugin);
    }
}