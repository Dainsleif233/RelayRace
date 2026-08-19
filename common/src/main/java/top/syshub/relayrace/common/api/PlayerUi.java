package top.syshub.relayrace.common.api;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Version-neutral player UI operations.
 * <p>
 * Common code passes MiniMessage-style strings. Each platform converts them to
 * the server-native text system (Adventure on latest, legacy chat on classic).
 */
public interface PlayerUi {

    void setPlayerListName(Player player, String miniMessage);

    void setPlayerDisplayName(Player player, String miniMessage);

    void clearDisplayName(Player player);

    void sendMessage(CommandSender sender, String miniMessage);

    void sendTitle(Player player, String titleMiniMessage, String subtitleMiniMessage,
                   int fadeInTicks, int stayTicks, int fadeOutTicks);

    BossBarHandle createBossBar(top.syshub.relayrace.common.RelayRacePlugin plugin,
                                String title, String color);
}