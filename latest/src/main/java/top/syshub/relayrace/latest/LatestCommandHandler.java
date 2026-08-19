package top.syshub.relayrace.latest;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import top.syshub.relayrace.common.GameManager;
import top.syshub.relayrace.common.RelayRacePlugin;
import top.syshub.relayrace.common.api.CommandRegistrar;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public final class LatestCommandHandler implements CommandRegistrar {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Override
    public void register(RelayRacePlugin plugin, GameManager gameManager) {
        plugin.getLifecycleManager().registerEventHandler(
            LifecycleEvents.COMMANDS,
            event -> event.registrar().register(
                buildCommandTree(plugin, gameManager),
                "RelayRace command",
                Collections.singletonList("rr")));
    }

    private static LiteralCommandNode<CommandSourceStack> buildCommandTree(
            RelayRacePlugin plugin, GameManager gameManager) {
        return Commands.literal("relayrace")
            .requires(source -> source.getSender().hasPermission("relayrace.command"))
            .then(Commands.literal("config")
                .then(Commands.literal("playtime")
                    .executes(ctx -> configPlaytimeGet(ctx, plugin, gameManager))
                    .then(Commands.argument("time", IntegerArgumentType.integer(1))
                        .executes(ctx -> configPlaytimeSet(ctx, gameManager))))
                .then(Commands.literal("debug")
                    .executes(ctx -> configDebugGet(ctx, plugin, gameManager))
                    .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(ctx -> configDebugSet(ctx, plugin, gameManager))))
                .then(Commands.literal("loop")
                    .executes(ctx -> configLoopGet(ctx, plugin, gameManager))
                    .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(ctx -> configLoopSet(ctx, plugin, gameManager))))
                .then(Commands.literal("freeze")
                    .executes(ctx -> configFreezeGet(ctx, plugin, gameManager))
                    .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(ctx -> configFreezeSet(ctx, plugin, gameManager))))
                .then(Commands.literal("externallobby")
                    .executes(ctx -> configExternalLobbyGet(ctx, plugin, gameManager))
                    .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(ctx -> configExternalLobbySet(ctx, gameManager))))
                .then(Commands.literal("externallobby-server")
                    .executes(ctx -> configExternalLobbyServerGet(ctx, plugin, gameManager))
                    .then(Commands.argument("server", StringArgumentType.word())
                        .executes(ctx -> configExternalLobbyServerSet(ctx, gameManager))))
                .then(Commands.literal("locales")
                    .executes(ctx -> configLocalesGet(ctx, gameManager))
                    .then(Commands.argument("locale", StringArgumentType.word())
                        .executes(ctx -> configLocalesSet(ctx, plugin, gameManager)))))
            .then(Commands.literal("sort")
                .executes(ctx -> sort(ctx, gameManager)))
            .then(Commands.literal("join")
                .then(Commands.argument("targets", ArgumentTypes.players())
                    .executes(ctx -> join(ctx, gameManager))))
            .then(Commands.literal("leave")
                .then(Commands.argument("targets", ArgumentTypes.players())
                    .executes(ctx -> leave(ctx, gameManager))))
            .then(Commands.literal("start")
                .executes(ctx -> start(ctx, gameManager)))
            .then(Commands.literal("next")
                .executes(ctx -> next(ctx, gameManager)))
            .then(Commands.literal("stop")
                .executes(ctx -> stop(ctx, gameManager)))
            .build();
    }

    private static int configPlaytimeGet(CommandContext<CommandSourceStack> ctx,
                                          RelayRacePlugin plugin, GameManager gm) {
        int seconds = plugin.getRelayConfig().getPlaytimeSeconds();
        send(ctx, gm.getTranslator().format("command.config.playtime.get", String.valueOf(seconds)));
        return Command.SINGLE_SUCCESS;
    }

    private static int configPlaytimeSet(CommandContext<CommandSourceStack> ctx,
                                         GameManager gm) {
        if (gm.isRunning()) {
            send(ctx, gm.getTranslator().format("command.config.playtime.running"));
            return Command.SINGLE_SUCCESS;
        }
        int seconds = ctx.getArgument("time", Integer.class);
        gm.setPlaytimeSeconds(seconds);
        send(ctx, gm.getTranslator().format("command.config.playtime.set", String.valueOf(seconds)));
        return Command.SINGLE_SUCCESS;
    }

    private static int sort(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        gm.sortWaiting();
        send(ctx, gm.getTranslator().format("command.sort.done"));
        return Command.SINGLE_SUCCESS;
    }

    private static int join(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        Collection<Player> targets;
        try {
            PlayerSelectorArgumentResolver resolver =
                ctx.getArgument("targets", PlayerSelectorArgumentResolver.class);
            targets = resolver.resolve(ctx.getSource());
        } catch (CommandSyntaxException e) {
            send(ctx, gm.getTranslator().format("command.select.invalid"));
            return Command.SINGLE_SUCCESS;
        }
        int count = 0;
        for (Player p : targets) {
            if (gm.isISpec(p)) {
                gm.addToWaiting(p);
                count++;
            }
        }
        send(ctx, gm.getTranslator().format("command.join.added", String.valueOf(count)));
        return count;
    }

    private static int leave(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        Collection<Player> targets;
        try {
            PlayerSelectorArgumentResolver resolver =
                ctx.getArgument("targets", PlayerSelectorArgumentResolver.class);
            targets = resolver.resolve(ctx.getSource());
        } catch (CommandSyntaxException e) {
            send(ctx, gm.getTranslator().format("command.select.invalid"));
            return Command.SINGLE_SUCCESS;
        }
        int count = 0;
        for (Player p : targets) {
            if (gm.isWaiting(p)) {
                gm.removeFromWaiting(p);
                count++;
            }
        }
        send(ctx, gm.getTranslator().format("command.leave.removed", String.valueOf(count)));
        return count;
    }

    private static int start(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        if (gm.isRunning()) {
            send(ctx, gm.getTranslator().format("command.start.running"));
            return Command.SINGLE_SUCCESS;
        }
        if (gm.startGame()) {
            send(ctx, gm.getTranslator().format("command.start.success"));
        } else if (gm.isRunning()) {
            send(ctx, gm.getTranslator().format("command.start.running"));
        } else {
            send(ctx, gm.getTranslator().format("command.start.failed"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int next(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        if (!gm.isRunning()) {
            send(ctx, gm.getTranslator().format("command.next.none"));
            return Command.SINGLE_SUCCESS;
        }
        if (gm.isPendingRotation()) {
            send(ctx, gm.getTranslator().format("command.next.pending"));
            return Command.SINGLE_SUCCESS;
        }
        gm.switchToNextPlayer();
        send(ctx, gm.getTranslator().format("command.next.success"));
        return Command.SINGLE_SUCCESS;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        if (!gm.isRunning()) {
            send(ctx, gm.getTranslator().format("command.stop.none"));
            return Command.SINGLE_SUCCESS;
        }
        gm.endGame(false);
        send(ctx, gm.getTranslator().format("command.stop.success"));
        return Command.SINGLE_SUCCESS;
    }

    private static int configDebugGet(CommandContext<CommandSourceStack> ctx,
                                      RelayRacePlugin plugin, GameManager gm) {
        send(ctx, gm.getTranslator().format("command.config.debug.status",
            String.valueOf(plugin.getRelayConfig().isDebug())));
        return Command.SINGLE_SUCCESS;
    }

    private static int configDebugSet(CommandContext<CommandSourceStack> ctx,
                                      RelayRacePlugin plugin, GameManager gm) {
        boolean enabled = ctx.getArgument("enable", Boolean.class);
        plugin.getRelayConfig().setDebug(enabled);
        send(ctx, gm.getTranslator().format("command.config.debug.status", String.valueOf(enabled)));
        return Command.SINGLE_SUCCESS;
    }

    private static int configLoopGet(CommandContext<CommandSourceStack> ctx,
                                     RelayRacePlugin plugin, GameManager gm) {
        send(ctx, gm.getTranslator().format("command.config.loop.status",
            String.valueOf(plugin.getRelayConfig().isLoop())));
        return Command.SINGLE_SUCCESS;
    }

    private static int configLoopSet(CommandContext<CommandSourceStack> ctx,
                                     RelayRacePlugin plugin, GameManager gm) {
        boolean enabled = ctx.getArgument("enable", Boolean.class);
        plugin.getRelayConfig().setLoop(enabled);
        send(ctx, gm.getTranslator().format("command.config.loop.status", String.valueOf(enabled)));
        return Command.SINGLE_SUCCESS;
    }

    private static int configLocalesGet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        send(ctx, gm.getTranslator().format("command.config.locales.current", gm.getTranslator().getCurrentLocale()));
        return Command.SINGLE_SUCCESS;
    }

    private static int configLocalesSet(CommandContext<CommandSourceStack> ctx,
                                        RelayRacePlugin plugin, GameManager gm) {
        String locale = ctx.getArgument("locale", String.class);
        Set<String> available = gm.getTranslator().getAvailableLocales();

        if (!available.contains(locale)) {
            String joined = String.join(", ", available);
            send(ctx, gm.getTranslator().format("command.config.locales.invalid", joined));
            return Command.SINGLE_SUCCESS;
        }

        plugin.getRelayConfig().setLocale(locale);
        gm.getTranslator().loadLocale(locale);
        send(ctx, gm.getTranslator().format("command.config.locales.changed", locale));
        return Command.SINGLE_SUCCESS;
    }

    private static int configFreezeGet(CommandContext<CommandSourceStack> ctx,
                                       RelayRacePlugin plugin, GameManager gm) {
        send(ctx, gm.getTranslator().format("command.config.freeze.status",
            String.valueOf(plugin.getRelayConfig().isFreeze())));
        return Command.SINGLE_SUCCESS;
    }

    private static int configFreezeSet(CommandContext<CommandSourceStack> ctx,
                                       RelayRacePlugin plugin, GameManager gm) {
        boolean enabled = ctx.getArgument("enable", Boolean.class);
        plugin.getRelayConfig().setFreeze(enabled);
        send(ctx, gm.getTranslator().format("command.config.freeze.status", String.valueOf(enabled)));
        return Command.SINGLE_SUCCESS;
    }

    private static int configExternalLobbyGet(CommandContext<CommandSourceStack> ctx,
                                              RelayRacePlugin plugin, GameManager gm) {
        send(ctx, gm.getTranslator().format("command.config.externallobby.status",
            String.valueOf(plugin.getRelayConfig().isExternalLobby())));
        return Command.SINGLE_SUCCESS;
    }

    private static int configExternalLobbySet(CommandContext<CommandSourceStack> ctx,
                                              GameManager gm) {
        boolean enabled = ctx.getArgument("enable", Boolean.class);
        gm.setExternalLobby(enabled);
        send(ctx, gm.getTranslator().format("command.config.externallobby.set", String.valueOf(enabled)));
        return Command.SINGLE_SUCCESS;
    }

    private static int configExternalLobbyServerGet(CommandContext<CommandSourceStack> ctx,
                                                    RelayRacePlugin plugin, GameManager gm) {
        send(ctx, gm.getTranslator().format("command.config.externallobbyServer.status",
            plugin.getRelayConfig().getExternalLobbyServer()));
        return Command.SINGLE_SUCCESS;
    }

    private static int configExternalLobbyServerSet(CommandContext<CommandSourceStack> ctx,
                                                    GameManager gm) {
        String server = ctx.getArgument("server", String.class);
        gm.setExternalLobbyServer(server);
        send(ctx, gm.getTranslator().format("command.config.externallobbyServer.set", server));
        return Command.SINGLE_SUCCESS;
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String message) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage(MINI_MESSAGE.deserialize(message == null ? "" : message));
    }
}