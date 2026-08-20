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
                    .executes(ctx -> {
                        configPlaytimeGet(plugin, gameManager, ctx);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("time", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            configPlaytimeSet(gameManager, ctx);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("debug")
                    .executes(ctx -> {
                        configDebugGet(plugin, gameManager, ctx);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(ctx -> {
                            configDebugSet(plugin, gameManager, ctx);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("loop")
                    .executes(ctx -> {
                        configLoopGet(plugin, gameManager, ctx);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(ctx -> {
                            configLoopSet(plugin, gameManager, ctx);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("freeze")
                    .executes(ctx -> {
                        configFreezeGet(plugin, gameManager, ctx);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(ctx -> {
                            configFreezeSet(plugin, gameManager, ctx);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("externallobby")
                    .executes(ctx -> {
                        configExternalLobbyGet(plugin, gameManager, ctx);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(ctx -> {
                            configExternalLobbySet(gameManager, ctx);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("externallobby-server")
                    .executes(ctx -> {
                        configExternalLobbyServerGet(plugin, gameManager, ctx);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("server", StringArgumentType.word())
                        .executes(ctx -> {
                            configExternalLobbyServerSet(gameManager, ctx);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("locales")
                    .executes(ctx -> {
                        configLocalesGet(gameManager, ctx);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("locale", StringArgumentType.word())
                        .executes(ctx -> {
                            configLocalesSet(plugin, gameManager, ctx);
                            return Command.SINGLE_SUCCESS;
                        }))))
            .then(Commands.literal("sort")
                .executes(ctx -> {
                    sort(gameManager, ctx);
                    return Command.SINGLE_SUCCESS;
                }))
            .then(Commands.literal("join")
                .then(Commands.argument("targets", ArgumentTypes.players())
                    .executes(ctx -> join(gameManager, ctx))))
            .then(Commands.literal("leave")
                .then(Commands.argument("targets", ArgumentTypes.players())
                    .executes(ctx -> leave(gameManager, ctx))))
            .then(Commands.literal("start")
                .executes(ctx -> {
                    start(gameManager, ctx);
                    return Command.SINGLE_SUCCESS;
                }))
            .then(Commands.literal("next")
                .executes(ctx -> {
                    next(gameManager, ctx);
                    return Command.SINGLE_SUCCESS;
                }))
            .then(Commands.literal("stop")
                .executes(ctx -> {
                    stop(gameManager, ctx);
                    return Command.SINGLE_SUCCESS;
                }))
            .build();
    }

    private static void configPlaytimeGet(RelayRacePlugin plugin, GameManager gm,
                                          CommandContext<CommandSourceStack> ctx) {
        int seconds = plugin.getRelayConfig().getPlaytimeSeconds();
        send(ctx, gm.getTranslator().format("command.config.playtime.get", String.valueOf(seconds)));
    }

    private static void configPlaytimeSet(GameManager gm,
                                          CommandContext<CommandSourceStack> ctx) {
        if (gm.isRunning()) {
            send(ctx, gm.getTranslator().format("command.config.playtime.running"));
            return;
        }
        int seconds = ctx.getArgument("time", Integer.class);
        gm.setPlaytimeSeconds(seconds);
        send(ctx, gm.getTranslator().format("command.config.playtime.set", String.valueOf(seconds)));
    }

    private static void sort(GameManager gm, CommandContext<CommandSourceStack> ctx) {
        gm.sortWaiting();
        send(ctx, gm.getTranslator().format("command.sort.done"));
    }

    private static int join(GameManager gm, CommandContext<CommandSourceStack> ctx) {
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

    private static int leave(GameManager gm, CommandContext<CommandSourceStack> ctx) {
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

    private static void start(GameManager gm, CommandContext<CommandSourceStack> ctx) {
        if (gm.isRunning()) {
            send(ctx, gm.getTranslator().format("command.start.running"));
            return;
        }
        if (gm.startGame()) {
            send(ctx, gm.getTranslator().format("command.start.success"));
        } else if (gm.isRunning()) {
            send(ctx, gm.getTranslator().format("command.start.running"));
        } else {
            send(ctx, gm.getTranslator().format("command.start.failed"));
        }
    }

    private static void next(GameManager gm, CommandContext<CommandSourceStack> ctx) {
        if (!gm.isRunning()) {
            send(ctx, gm.getTranslator().format("command.next.none"));
            return;
        }
        if (gm.isPendingRotation()) {
            send(ctx, gm.getTranslator().format("command.next.pending"));
            return;
        }
        gm.switchToNextPlayer();
        send(ctx, gm.getTranslator().format("command.next.success"));
    }

    private static void stop(GameManager gm, CommandContext<CommandSourceStack> ctx) {
        if (!gm.isRunning()) {
            send(ctx, gm.getTranslator().format("command.stop.none"));
            return;
        }
        gm.endGame(false);
        send(ctx, gm.getTranslator().format("command.stop.success"));
    }

    private static void configDebugGet(RelayRacePlugin plugin, GameManager gm,
                                       CommandContext<CommandSourceStack> ctx) {
        send(ctx, gm.getTranslator().format("command.config.debug.status",
            String.valueOf(plugin.getRelayConfig().isDebug())));
    }

    private static void configDebugSet(RelayRacePlugin plugin, GameManager gm,
                                       CommandContext<CommandSourceStack> ctx) {
        boolean enabled = ctx.getArgument("enable", Boolean.class);
        plugin.getRelayConfig().setDebug(enabled);
        send(ctx, gm.getTranslator().format("command.config.debug.status", String.valueOf(enabled)));
    }

    private static void configLoopGet(RelayRacePlugin plugin, GameManager gm,
                                      CommandContext<CommandSourceStack> ctx) {
        send(ctx, gm.getTranslator().format("command.config.loop.status",
            String.valueOf(plugin.getRelayConfig().isLoop())));
    }

    private static void configLoopSet(RelayRacePlugin plugin, GameManager gm,
                                      CommandContext<CommandSourceStack> ctx) {
        boolean enabled = ctx.getArgument("enable", Boolean.class);
        plugin.getRelayConfig().setLoop(enabled);
        send(ctx, gm.getTranslator().format("command.config.loop.status", String.valueOf(enabled)));
    }

    private static void configLocalesGet(GameManager gm,
                                         CommandContext<CommandSourceStack> ctx) {
        send(ctx, gm.getTranslator().format("command.config.locales.current", gm.getTranslator().getCurrentLocale()));
    }

    private static void configLocalesSet(RelayRacePlugin plugin, GameManager gm,
                                         CommandContext<CommandSourceStack> ctx) {
        String locale = ctx.getArgument("locale", String.class);
        Set<String> available = gm.getTranslator().getAvailableLocales();

        if (!available.contains(locale)) {
            String joined = String.join(", ", available);
            send(ctx, gm.getTranslator().format("command.config.locales.invalid", joined));
            return;
        }

        plugin.getRelayConfig().setLocale(locale);
        gm.getTranslator().loadLocale(locale);
        send(ctx, gm.getTranslator().format("command.config.locales.changed", locale));
    }

    private static void configFreezeGet(RelayRacePlugin plugin, GameManager gm,
                                        CommandContext<CommandSourceStack> ctx) {
        send(ctx, gm.getTranslator().format("command.config.freeze.status",
            String.valueOf(plugin.getRelayConfig().isFreeze())));
    }

    private static void configFreezeSet(RelayRacePlugin plugin, GameManager gm,
                                        CommandContext<CommandSourceStack> ctx) {
        boolean enabled = ctx.getArgument("enable", Boolean.class);
        plugin.getRelayConfig().setFreeze(enabled);
        send(ctx, gm.getTranslator().format("command.config.freeze.status", String.valueOf(enabled)));
    }

    private static void configExternalLobbyGet(RelayRacePlugin plugin, GameManager gm,
                                               CommandContext<CommandSourceStack> ctx) {
        send(ctx, gm.getTranslator().format("command.config.externallobby.status",
            String.valueOf(plugin.getRelayConfig().isExternalLobby())));
    }

    private static void configExternalLobbySet(GameManager gm,
                                               CommandContext<CommandSourceStack> ctx) {
        boolean enabled = ctx.getArgument("enable", Boolean.class);
        gm.setExternalLobby(enabled);
        send(ctx, gm.getTranslator().format("command.config.externallobby.set", String.valueOf(enabled)));
    }

    private static void configExternalLobbyServerGet(RelayRacePlugin plugin, GameManager gm,
                                                     CommandContext<CommandSourceStack> ctx) {
        send(ctx, gm.getTranslator().format("command.config.externallobbyServer.status",
            plugin.getRelayConfig().getExternalLobbyServer()));
    }

    private static void configExternalLobbyServerSet(GameManager gm,
                                                     CommandContext<CommandSourceStack> ctx) {
        String server = ctx.getArgument("server", String.class);
        gm.setExternalLobbyServer(server);
        send(ctx, gm.getTranslator().format("command.config.externallobbyServer.set", server));
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String message) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage(MINI_MESSAGE.deserialize(message == null ? "" : message));
    }
}
