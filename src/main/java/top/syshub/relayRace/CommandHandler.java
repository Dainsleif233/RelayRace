package top.syshub.relayRace;

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
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class CommandHandler {

    private CommandHandler() {}

    public static void register(RelayRace plugin, GameManager gameManager) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(buildCommandTree(gameManager), "RelayRace command", List.of("rr")));
    }

    private static LiteralCommandNode<CommandSourceStack> buildCommandTree(GameManager gameManager) {
        return Commands.literal("relayrace")
            .requires(source -> source.getSender().hasPermission("relayrace.command"))
            .then(Commands.literal("config")
                .then(Commands.literal("playtime")
                    .executes(ctx -> configPlaytimeGet(ctx, gameManager))
                    .then(Commands.argument("time", IntegerArgumentType.integer(1))
                        .executes(ctx -> configPlaytimeSet(ctx, gameManager))))
                .then(Commands.literal("debug")
                    .executes(ctx -> configDebugGet(ctx, gameManager))
                    .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(ctx -> configDebugSet(ctx, gameManager))))
                .then(Commands.literal("loop")
                    .executes(ctx -> configLoopGet(ctx, gameManager))
                    .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(ctx -> configLoopSet(ctx, gameManager))))
                .then(Commands.literal("freeze")
                    .executes(ctx -> configFreezeGet(ctx, gameManager))
                    .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(ctx -> configFreezeSet(ctx, gameManager))))
                .then(Commands.literal("externallobby")
                    .executes(ctx -> configExternalLobbyGet(ctx, gameManager))
                    .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(ctx -> configExternalLobbySet(ctx, gameManager))))
                .then(Commands.literal("externallobby-server")
                    .executes(ctx -> configExternalLobbyServerGet(ctx, gameManager))
                    .then(Commands.argument("server", StringArgumentType.word())
                        .executes(ctx -> configExternalLobbyServerSet(ctx, gameManager))))
                .then(Commands.literal("locales")
                    .executes(ctx -> configLocalesGet(ctx, gameManager))
                    .then(Commands.argument("locale", StringArgumentType.word())
                        .executes(ctx -> configLocalesSet(ctx, gameManager)))))
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

    private static int configPlaytimeGet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        int seconds = gm.getPlaytimeSeconds();
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.config.playtime.get", String.valueOf(seconds)));
        return Command.SINGLE_SUCCESS;
    }

    private static int configPlaytimeSet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        if (gm.isRunning()) {
            ctx.getSource().getSender().sendMessage(
                gm.getTranslator().translate("command.config.playtime.running"));
            return Command.SINGLE_SUCCESS;
        }
        int seconds = ctx.getArgument("time", Integer.class);
        gm.setPlaytimeSeconds(seconds);
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.config.playtime.set", String.valueOf(seconds)));
        return Command.SINGLE_SUCCESS;
    }

    private static int sort(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        gm.sortWaiting();
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.sort.done"));
        return Command.SINGLE_SUCCESS;
    }

    private static int join(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        Collection<Player> targets;
        try {
            PlayerSelectorArgumentResolver resolver = ctx.getArgument("targets", PlayerSelectorArgumentResolver.class);
            targets = resolver.resolve(ctx.getSource());
        } catch (CommandSyntaxException e) {
            ctx.getSource().getSender().sendMessage(
                gm.getTranslator().translate("command.select.invalid"));
            return Command.SINGLE_SUCCESS;
        }
        int count = 0;
        for (Player p : targets) {
            if (gm.isISpec(p)) {
                gm.addToWaiting(p);
                count++;
            }
        }
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.join.added", String.valueOf(count)));
        return count;
    }

    private static int leave(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        Collection<Player> targets;
        try {
            PlayerSelectorArgumentResolver resolver = ctx.getArgument("targets", PlayerSelectorArgumentResolver.class);
            targets = resolver.resolve(ctx.getSource());
        } catch (CommandSyntaxException e) {
            ctx.getSource().getSender().sendMessage(
                gm.getTranslator().translate("command.select.invalid"));
            return Command.SINGLE_SUCCESS;
        }
        int count = 0;
        for (Player p : targets) {
            if (gm.isWaiting(p)) {
                gm.removeFromWaiting(p);
                count++;
            }
        }
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.leave.removed", String.valueOf(count)));
        return count;
    }

    private static int start(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        if (gm.isRunning()) {
            ctx.getSource().getSender().sendMessage(
                gm.getTranslator().translate("command.start.running"));
            return Command.SINGLE_SUCCESS;
        }
        if (gm.startGame()) {
            ctx.getSource().getSender().sendMessage(
                gm.getTranslator().translate("command.start.success"));
        } else if (gm.isRunning()) {
            ctx.getSource().getSender().sendMessage(
                gm.getTranslator().translate("command.start.running"));
        } else {
            ctx.getSource().getSender().sendMessage(
                gm.getTranslator().translate("command.start.failed"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int next(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        if (!gm.isRunning()) {
            ctx.getSource().getSender().sendMessage(
                gm.getTranslator().translate("command.next.none"));
            return Command.SINGLE_SUCCESS;
        }
        if (gm.isPendingRotation()) {
            ctx.getSource().getSender().sendMessage(
                gm.getTranslator().translate("command.next.pending"));
            return Command.SINGLE_SUCCESS;
        }
        gm.switchToNextPlayer();
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.next.success"));
        return Command.SINGLE_SUCCESS;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        if (!gm.isRunning()) {
            ctx.getSource().getSender().sendMessage(
                gm.getTranslator().translate("command.stop.none"));
            return Command.SINGLE_SUCCESS;
        }
        gm.endGame(false);
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.stop.success"));
        return Command.SINGLE_SUCCESS;
    }

    private static int configDebugGet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.config.debug.status", String.valueOf(gm.isDebug())));
        return Command.SINGLE_SUCCESS;
    }

    private static int configDebugSet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        boolean enabled = ctx.getArgument("enable", Boolean.class);
        gm.setDebug(enabled);
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.config.debug.status", String.valueOf(enabled)));
        return Command.SINGLE_SUCCESS;
    }

    private static int configLoopGet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.config.loop.status", String.valueOf(gm.isLoop())));
        return Command.SINGLE_SUCCESS;
    }

    private static int configLoopSet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        boolean enabled = ctx.getArgument("enable", Boolean.class);
        gm.setLoop(enabled);
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.config.loop.status", String.valueOf(enabled)));
        return Command.SINGLE_SUCCESS;
    }

    private static int configLocalesGet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        String current = gm.getTranslator().getCurrentLocale();
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.config.locales.current", current));
        return Command.SINGLE_SUCCESS;
    }

    private static int configLocalesSet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        String locale = ctx.getArgument("locale", String.class);
        Set<String> available = gm.getTranslator().getAvailableLocales();

        if (!available.contains(locale)) {
            String joined = String.join(", ", available);
            ctx.getSource().getSender().sendMessage(
                gm.getTranslator().translate("command.config.locales.invalid", joined));
            return Command.SINGLE_SUCCESS;
        }

        gm.setLocale(locale);
        gm.getTranslator().loadLocale(locale);
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.config.locales.changed", locale));
        return Command.SINGLE_SUCCESS;
    }

    private static int configFreezeGet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.config.freeze.status", String.valueOf(gm.isFreeze())));
        return Command.SINGLE_SUCCESS;
    }

    private static int configFreezeSet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        boolean enabled = ctx.getArgument("enable", Boolean.class);
        gm.setFreeze(enabled);
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.config.freeze.status", String.valueOf(enabled)));
        return Command.SINGLE_SUCCESS;
    }

    private static int configExternalLobbyGet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.config.externallobby.status", String.valueOf(gm.isExternalLobby())));
        return Command.SINGLE_SUCCESS;
    }

    private static int configExternalLobbySet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        boolean enabled = ctx.getArgument("enable", Boolean.class);
        gm.setExternalLobby(enabled);
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.config.externallobby.set", String.valueOf(enabled)));
        return Command.SINGLE_SUCCESS;
    }

    private static int configExternalLobbyServerGet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.config.externallobbyServer.status", gm.getExternalLobbyServer()));
        return Command.SINGLE_SUCCESS;
    }

    private static int configExternalLobbyServerSet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        String server = ctx.getArgument("server", String.class);
        gm.setExternalLobbyServer(server);
        ctx.getSource().getSender().sendMessage(
            gm.getTranslator().translate("command.config.externallobbyServer.set", server));
        return Command.SINGLE_SUCCESS;
    }
}
