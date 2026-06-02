package top.syshub.relayRace;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

public final class CommandHandler {

    private CommandHandler() {}

    public static void register(RelayRace plugin, GameManager gameManager) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(buildCommandTree(gameManager), "RelayRace command", List.of("rr")));
    }

    private static LiteralCommandNode<CommandSourceStack> buildCommandTree(GameManager gameManager) {
        return Commands.literal("relayrace")
            .requires(source -> source.getSender().hasPermission("relayrace.command"))
            .then(Commands.literal("playtime")
                .executes(ctx -> playtimeGet(ctx, gameManager))
                .then(Commands.argument("time", IntegerArgumentType.integer(1))
                    .executes(ctx -> playtimeSet(ctx, gameManager))))
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
            .then(Commands.literal("debug")
                .executes(ctx -> debugGet(ctx, gameManager))
                .then(Commands.argument("enable", BoolArgumentType.bool())
                    .executes(ctx -> debugSet(ctx, gameManager))))
            .then(Commands.literal("loop")
                .executes(ctx -> loopGet(ctx, gameManager))
                .then(Commands.argument("enable", BoolArgumentType.bool())
                    .executes(ctx -> loopSet(ctx, gameManager))))
            .build();
    }

    private static int playtimeGet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        int seconds = gm.getPlaytimeSeconds();
        ctx.getSource().getSender().sendMessage(
            Component.text("Current playtime: " + seconds + " seconds", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int playtimeSet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        if (gm.isRunning()) {
            ctx.getSource().getSender().sendMessage(
                Component.text("Cannot change playtime while game is running.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        int seconds = ctx.getArgument("time", Integer.class);
        gm.setPlaytimeSeconds(seconds);
        ctx.getSource().getSender().sendMessage(
            Component.text("Playtime set to " + seconds + " seconds.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int sort(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        gm.sortWaiting();
        ctx.getSource().getSender().sendMessage(
            Component.text("Waiting players have been randomly sorted.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int join(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        Collection<Player> targets;
        try {
            PlayerSelectorArgumentResolver resolver = ctx.getArgument("targets", PlayerSelectorArgumentResolver.class);
            targets = resolver.resolve(ctx.getSource());
        } catch (CommandSyntaxException e) {
            ctx.getSource().getSender().sendMessage(
                Component.text("Invalid player selector.", NamedTextColor.RED));
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
            Component.text("Added " + count + " player(s) to waiting queue.", NamedTextColor.GREEN));
        return count;
    }

    private static int leave(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        Collection<Player> targets;
        try {
            PlayerSelectorArgumentResolver resolver = ctx.getArgument("targets", PlayerSelectorArgumentResolver.class);
            targets = resolver.resolve(ctx.getSource());
        } catch (CommandSyntaxException e) {
            ctx.getSource().getSender().sendMessage(
                Component.text("Invalid player selector.", NamedTextColor.RED));
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
            Component.text("Removed " + count + " player(s) from waiting queue.", NamedTextColor.GREEN));
        return count;
    }

    private static int start(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        if (gm.isRunning()) {
            ctx.getSource().getSender().sendMessage(
                Component.text("Game is already running.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        if (gm.startGame()) {
            ctx.getSource().getSender().sendMessage(
                Component.text("Game started!", NamedTextColor.GREEN));
        } else if (gm.isRunning()) {
            ctx.getSource().getSender().sendMessage(
                Component.text("Game is already running.", NamedTextColor.RED));
        } else {
            ctx.getSource().getSender().sendMessage(
                Component.text("Failed to start. Add players with /rr join first.", NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int next(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        if (!gm.isRunning()) {
            ctx.getSource().getSender().sendMessage(
                Component.text("No game is running.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        gm.switchToNextPlayer();
        ctx.getSource().getSender().sendMessage(
            Component.text("Switched to next player.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        if (!gm.isRunning()) {
            ctx.getSource().getSender().sendMessage(
                Component.text("No game is running.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        gm.endGame(false);
        ctx.getSource().getSender().sendMessage(
            Component.text("Game stopped.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int debugGet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        ctx.getSource().getSender().sendMessage(
            Component.text("Debug mode: " + gm.isDebug(), NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int debugSet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        boolean enabled = ctx.getArgument("enable", Boolean.class);
        gm.setDebug(enabled);
        ctx.getSource().getSender().sendMessage(
            Component.text("Debug mode: " + enabled, NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int loopGet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        ctx.getSource().getSender().sendMessage(
            Component.text("Loop mode: " + gm.isLoop(), NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int loopSet(CommandContext<CommandSourceStack> ctx, GameManager gm) {
        boolean enabled = ctx.getArgument("enable", Boolean.class);
        gm.setLoop(enabled);
        ctx.getSource().getSender().sendMessage(
            Component.text("Loop mode: " + enabled, NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }
}
