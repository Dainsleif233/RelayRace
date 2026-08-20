package top.syshub.relayrace.classic;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.jorel.commandapi.executors.ResultingCommandExecutor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import top.syshub.relayrace.common.GameManager;
import top.syshub.relayrace.common.RelayRacePlugin;
import top.syshub.relayrace.common.api.CommandRegistrar;

import java.util.Collection;
import java.util.Set;

/**
 * CommandAPI-based command registration for the classic (1.16.1) branch.
 *
 * <p>The commands mirror the Paper-Brigadier tree used on the latest branch,
 * but are built on the shaded CommandAPI library (5.12) because 1.16.1 has no
 * stable multi-version Brigadier API. CommandAPI is shaded into the final jar,
 * so no external CommandAPI plugin is required.
 */
public final class ClassicCommandHandler implements CommandRegistrar {

    private static final String PERMISSION = "relayrace.command";

    private RelayRacePlugin plugin;
    private GameManager gm;

    @Override
    public void register(RelayRacePlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gm = gameManager;

        // Finish CommandAPI setup (listener + post-load permission fixing).
        // Loading itself happens once from ClassicPlatform#onLoad().
        CommandAPI.onEnable(plugin);

        CommandAPICommand root = command("relayrace")
            .withAliases("rr")
            .withPermission(PERMISSION)
            .withSubcommand(config())
            .withSubcommand(command("sort").executes((sender, args) -> {
                gm.sortWaiting();
                send(sender, "command.sort.done");
            }))
            .withSubcommand(command("join")
                .withArguments(new EntitySelectorArgument("targets", EntitySelector.MANY_PLAYERS))
                .executes((ResultingCommandExecutor) (sender, args) ->
                    join(sender, castPlayers(args[0]))))
            .withSubcommand(command("leave")
                .withArguments(new EntitySelectorArgument("targets", EntitySelector.MANY_PLAYERS))
                .executes((ResultingCommandExecutor) (sender, args) ->
                    leave(sender, castPlayers(args[0]))))
            .withSubcommand(command("start")
                .executes((CommandExecutor) (sender, args) -> start(sender)))
            .withSubcommand(command("next")
                .executes((CommandExecutor) (sender, args) -> next(sender)))
            .withSubcommand(command("stop")
                .executes((CommandExecutor) (sender, args) -> stop(sender)));

        root.register();
    }

    private CommandAPICommand config() {
        return command("config")
            .withSubcommand(command("playtime")
                .executes((CommandExecutor) (sender, args) -> playtimeGet(sender)))
            .withSubcommand(command("playtime")
                .withArguments(new IntegerArgument("time", 1))
                .executes((CommandExecutor) (sender, args) -> playtimeSet(sender, (Integer) args[0])))
            .withSubcommand(command("debug")
                .executes((CommandExecutor) (sender, args) -> debugGet(sender)))
            .withSubcommand(command("debug")
                .withArguments(new BooleanArgument("enable"))
                .executes((CommandExecutor) (sender, args) -> debugSet(sender, (Boolean) args[0])))
            .withSubcommand(command("loop")
                .executes((CommandExecutor) (sender, args) -> loopGet(sender)))
            .withSubcommand(command("loop")
                .withArguments(new BooleanArgument("enable"))
                .executes((CommandExecutor) (sender, args) -> loopSet(sender, (Boolean) args[0])))
            .withSubcommand(command("freeze")
                .executes((CommandExecutor) (sender, args) -> freezeGet(sender)))
            .withSubcommand(command("freeze")
                .withArguments(new BooleanArgument("enable"))
                .executes((CommandExecutor) (sender, args) -> freezeSet(sender, (Boolean) args[0])))
            .withSubcommand(command("externallobby")
                .executes((CommandExecutor) (sender, args) -> externalLobbyGet(sender)))
            .withSubcommand(command("externallobby")
                .withArguments(new BooleanArgument("enable"))
                .executes((CommandExecutor) (sender, args) -> externalLobbySet(sender, (Boolean) args[0])))
            .withSubcommand(command("externallobby-server")
                .executes((CommandExecutor) (sender, args) -> externalLobbyServerGet(sender)))
            .withSubcommand(command("externallobby-server")
                .withArguments(new StringArgument("server"))
                .executes((CommandExecutor) (sender, args) -> externalLobbyServerSet(sender, (String) args[0])))
            .withSubcommand(command("locales")
                .executes((CommandExecutor) (sender, args) -> localesGet(sender)))
            .withSubcommand(command("locales")
                .withArguments(new StringArgument("locale"))
                .executes((CommandExecutor) (sender, args) -> localesSet(sender, (String) args[0])));
    }

    private void playtimeGet(CommandSender sender) {
        send(sender, "command.config.playtime.get",
            String.valueOf(plugin.getRelayConfig().getPlaytimeSeconds()));
    }

    private void playtimeSet(CommandSender sender, int seconds) {
        if (gm.isRunning()) {
            send(sender, "command.config.playtime.running");
            return;
        }
        gm.setPlaytimeSeconds(seconds);
        send(sender, "command.config.playtime.set", String.valueOf(seconds));
    }

    private void debugGet(CommandSender sender) {
        send(sender, "command.config.debug.status",
            String.valueOf(plugin.getRelayConfig().isDebug()));
    }

    private void debugSet(CommandSender sender, boolean enabled) {
        plugin.getRelayConfig().setDebug(enabled);
        send(sender, "command.config.debug.status", String.valueOf(enabled));
    }

    private void loopGet(CommandSender sender) {
        send(sender, "command.config.loop.status",
            String.valueOf(plugin.getRelayConfig().isLoop()));
    }

    private void loopSet(CommandSender sender, boolean enabled) {
        plugin.getRelayConfig().setLoop(enabled);
        send(sender, "command.config.loop.status", String.valueOf(enabled));
    }

    private void freezeGet(CommandSender sender) {
        send(sender, "command.config.freeze.status",
            String.valueOf(plugin.getRelayConfig().isFreeze()));
    }

    private void freezeSet(CommandSender sender, boolean enabled) {
        plugin.getRelayConfig().setFreeze(enabled);
        send(sender, "command.config.freeze.status", String.valueOf(enabled));
    }

    private void externalLobbyGet(CommandSender sender) {
        send(sender, "command.config.externallobby.status",
            String.valueOf(plugin.getRelayConfig().isExternalLobby()));
    }

    private void externalLobbySet(CommandSender sender, boolean enabled) {
        gm.setExternalLobby(enabled);
        send(sender, "command.config.externallobby.set", String.valueOf(enabled));
    }

    private void externalLobbyServerGet(CommandSender sender) {
        send(sender, "command.config.externallobbyServer.status",
            plugin.getRelayConfig().getExternalLobbyServer());
    }

    private void externalLobbyServerSet(CommandSender sender, String server) {
        gm.setExternalLobbyServer(server);
        send(sender, "command.config.externallobbyServer.set", server);
    }

    private void localesGet(CommandSender sender) {
        send(sender, "command.config.locales.current",
            gm.getTranslator().getCurrentLocale());
    }

    private void localesSet(CommandSender sender, String locale) {
        Set<String> available = gm.getTranslator().getAvailableLocales();
        if (!available.contains(locale)) {
            send(sender, "command.config.locales.invalid", String.join(", ", available));
            return;
        }
        plugin.getRelayConfig().setLocale(locale);
        gm.getTranslator().loadLocale(locale);
        send(sender, "command.config.locales.changed", locale);
    }

    private int join(CommandSender sender, Collection<Player> targets) {
        int count = 0;
        for (Player p : targets) {
            if (gm.isISpec(p)) {
                gm.addToWaiting(p);
                count++;
            }
        }
        send(sender, "command.join.added", String.valueOf(count));
        return count;
    }

    private int leave(CommandSender sender, Collection<Player> targets) {
        int count = 0;
        for (Player p : targets) {
            if (gm.isWaiting(p)) {
                gm.removeFromWaiting(p);
                count++;
            }
        }
        send(sender, "command.leave.removed", String.valueOf(count));
        return count;
    }

    private void start(CommandSender sender) {
        if (gm.isRunning()) {
            send(sender, "command.start.running");
        } else if (gm.startGame()) {
            send(sender, "command.start.success");
        } else if (gm.isRunning()) {
            send(sender, "command.start.running");
        } else {
            send(sender, "command.start.failed");
        }
    }

    private void next(CommandSender sender) {
        if (!gm.isRunning()) {
            send(sender, "command.next.none");
            return;
        }
        if (gm.isPendingRotation()) {
            send(sender, "command.next.pending");
            return;
        }
        gm.switchToNextPlayer();
        send(sender, "command.next.success");
    }

    private void stop(CommandSender sender) {
        if (!gm.isRunning()) {
            send(sender, "command.stop.none");
            return;
        }
        gm.endGame(false);
        send(sender, "command.stop.success");
    }

    private CommandAPICommand command(String name) {
        return new CommandAPICommand(name);
    }

    @SuppressWarnings("unchecked")
    private static Collection<Player> castPlayers(Object value) {
        return (Collection<Player>) value;
    }

    private void send(CommandSender sender, String key, String... args) {
        plugin.getPlatform().ui().sendMessage(sender, plugin.getTranslator().format(key, args));
    }
}
