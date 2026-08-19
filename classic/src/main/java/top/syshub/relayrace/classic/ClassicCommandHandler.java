package top.syshub.relayrace.classic;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;

import top.syshub.relayrace.common.GameManager;
import top.syshub.relayrace.common.RelayRacePlugin;
import top.syshub.relayrace.common.api.CommandRegistrar;

import java.lang.reflect.Field;
import java.util.*;

public final class ClassicCommandHandler implements CommandRegistrar {

    @Override
    public void register(RelayRacePlugin plugin, GameManager gameManager) {
        PluginCommand command;
        try {
            java.lang.reflect.Constructor<? extends PluginCommand> ctor =
                PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            ctor.setAccessible(true);
            command = ctor.newInstance("relayrace", plugin);
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to create RelayRace command on classic: " + e.getMessage());
            return;
        }

        command.setDescription("RelayRace commands");
        command.setAliases(Collections.singletonList("rr"));
        command.setPermission("relayrace.command");
        command.setUsage("/relayrace <join|leave|start|next|stop|sort|config>");
        command.setExecutor((sender, cmd, label, args) -> execute(plugin, gameManager, sender, args));

        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get(Bukkit.getServer());
            commandMap.register("relayrace", command);
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to register RelayRace command on classic: " + e.getMessage());
        }
    }

    private boolean execute(RelayRacePlugin plugin, GameManager gm,
                            CommandSender sender, String[] args) {
        if (!sender.hasPermission("relayrace.command")) {
            send(plugin, sender, "command.select.invalid");
            return true;
        }

        if (args.length == 0) {
            send(plugin, sender, "[missing: command.help]");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "config":
                return config(plugin, gm, sender, args);
            case "sort":
                gm.sortWaiting();
                send(plugin, sender, "command.sort.done");
                return true;
            case "join": {
                List<Player> targets = resolveTargets(plugin, sender, args);
                if (targets == null) return true;
                int count = 0;
                for (Player p : targets) {
                    if (gm.isISpec(p)) {
                        gm.addToWaiting(p);
                        count++;
                    }
                }
                send(plugin, sender, "command.join.added", String.valueOf(count));
                return true;
            }
            case "leave": {
                List<Player> targets = resolveTargets(plugin, sender, args);
                if (targets == null) return true;
                int count = 0;
                for (Player p : targets) {
                    if (gm.isWaiting(p)) {
                        gm.removeFromWaiting(p);
                        count++;
                    }
                }
                send(plugin, sender, "command.leave.removed", String.valueOf(count));
                return true;
            }
            case "start":
                if (gm.isRunning()) {
                    send(plugin, sender, "command.start.running");
                } else if (gm.startGame()) {
                    send(plugin, sender, "command.start.success");
                } else if (gm.isRunning()) {
                    send(plugin, sender, "command.start.running");
                } else {
                    send(plugin, sender, "command.start.failed");
                }
                return true;
            case "next":
                if (!gm.isRunning()) {
                    send(plugin, sender, "command.next.none");
                } else if (gm.isPendingRotation()) {
                    send(plugin, sender, "command.next.pending");
                } else {
                    gm.switchToNextPlayer();
                    send(plugin, sender, "command.next.success");
                }
                return true;
            case "stop":
                if (!gm.isRunning()) {
                    send(plugin, sender, "command.stop.none");
                } else {
                    gm.endGame(false);
                    send(plugin, sender, "command.stop.success");
                }
                return true;
        }

        send(plugin, sender, "command.select.invalid");
        return true;
    }

    private boolean config(RelayRacePlugin plugin, GameManager gm,
                           CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(plugin, sender, "command.select.invalid");
            return true;
        }

        String key = args[1].toLowerCase();

        switch (key) {
            case "playtime":
                if (args.length < 3) {
                    send(plugin, sender, "command.config.playtime.get",
                            String.valueOf(plugin.getRelayConfig().getPlaytimeSeconds()));
                } else if (gm.isRunning()) {
                    send(plugin, sender, "command.config.playtime.running");
                } else {
                    try {
                        int seconds = Integer.parseInt(args[2]);
                        gm.setPlaytimeSeconds(seconds);
                        send(plugin, sender, "command.config.playtime.set", String.valueOf(seconds));
                    } catch (NumberFormatException e) {
                        send(plugin, sender, "command.select.invalid");
                    }
                }
                return true;
            case "debug": {
                boolean enabled = args.length >= 3 ? Boolean.parseBoolean(args[2])
                        : plugin.getRelayConfig().isDebug();
                if (args.length >= 3) {
                    plugin.getRelayConfig().setDebug(enabled);
                }
                send(plugin, sender, "command.config.debug.status", String.valueOf(enabled));
                return true;
            }
            case "loop": {
                boolean enabled = args.length >= 3 ? Boolean.parseBoolean(args[2])
                        : plugin.getRelayConfig().isLoop();
                if (args.length >= 3) {
                    plugin.getRelayConfig().setLoop(enabled);
                }
                send(plugin, sender, "command.config.loop.status", String.valueOf(enabled));
                return true;
            }
            case "freeze": {
                boolean enabled = args.length >= 3 ? Boolean.parseBoolean(args[2])
                        : plugin.getRelayConfig().isFreeze();
                if (args.length >= 3) {
                    plugin.getRelayConfig().setFreeze(enabled);
                }
                send(plugin, sender, "command.config.freeze.status", String.valueOf(enabled));
                return true;
            }
            case "externallobby": {
                boolean enabled = args.length >= 3 ? Boolean.parseBoolean(args[2])
                        : plugin.getRelayConfig().isExternalLobby();
                if (args.length >= 3) {
                    gm.setExternalLobby(enabled);
                }
                send(plugin, sender, "command.config.externallobby.set", String.valueOf(enabled));
                return true;
            }
            case "externallobby-server":
                if (args.length < 3) {
                    send(plugin, sender, "command.config.externallobbyServer.status",
                            plugin.getRelayConfig().getExternalLobbyServer());
                } else {
                    String server = args[2];
                    gm.setExternalLobbyServer(server);
                    send(plugin, sender, "command.config.externallobbyServer.set", server);
                }
                return true;
            case "locales":
                if (args.length < 3) {
                    send(plugin, sender, "command.config.locales.current",
                            gm.getTranslator().getCurrentLocale());
                } else {
                    String locale = args[2];
                    Set<String> available = gm.getTranslator().getAvailableLocales();
                    if (!available.contains(locale)) {
                        send(plugin, sender, "command.config.locales.invalid",
                                String.join(", ", available));
                    } else {
                        plugin.getRelayConfig().setLocale(locale);
                        gm.getTranslator().loadLocale(locale);
                        send(plugin, sender, "command.config.locales.changed", locale);
                    }
                }
                return true;
        }

        send(plugin, sender, "command.select.invalid");
        return true;
    }

    private List<Player> resolveTargets(RelayRacePlugin plugin,
                                        CommandSender sender, String[] args) {
        List<Player> targets = new ArrayList<>();
        if (args.length >= 2) {
            for (int i = 1; i < args.length; i++) {
                Player p = Bukkit.getPlayerExact(args[i]);
                if (p != null) {
                    targets.add(p);
                }
            }
        } else if (sender instanceof Player) {
            targets.add((Player) sender);
        } else {
            send(plugin, sender, "command.select.invalid");
            return null;
        }
        if (targets.isEmpty()) {
            send(plugin, sender, "command.select.invalid");
            return null;
        }
        return targets;
    }

    private void send(RelayRacePlugin plugin, CommandSender sender, String key, String... args) {
        plugin.getPlatform().ui().sendMessage(sender, plugin.getTranslator().format(key, args));
    }
}