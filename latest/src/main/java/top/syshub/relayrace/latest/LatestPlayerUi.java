package top.syshub.relayrace.latest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import top.syshub.relayrace.common.RelayRacePlugin;
import top.syshub.relayrace.common.api.BossBarHandle;
import top.syshub.relayrace.common.api.PlayerUi;

import java.time.Duration;

public final class LatestPlayerUi implements PlayerUi {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void setPlayerListName(Player player, String miniMessage) {
        player.playerListName(deserialize(miniMessage));
    }

    @Override
    public void setPlayerDisplayName(Player player, String miniMessage) {
        player.displayName(deserialize(miniMessage));
    }

    @Override
    public void clearDisplayName(Player player) {
        player.playerListName(null);
        player.displayName(null);
    }

    @Override
    public void sendMessage(CommandSender sender, String miniMessage) {
        sender.sendMessage(deserialize(miniMessage));
    }

    @Override
    public void sendTitle(Player player, String titleMiniMessage, String subtitleMiniMessage,
                          int fadeInTicks, int stayTicks, int fadeOutTicks) {
        Title title = Title.title(
            deserialize(titleMiniMessage),
            deserialize(subtitleMiniMessage),
            Title.Times.times(
                Duration.ofMillis(fadeInTicks * 50L),
                Duration.ofMillis(stayTicks * 50L),
                Duration.ofMillis(fadeOutTicks * 50L)));
        player.showTitle(title);
    }

    @Override
    public BossBarHandle createBossBar(RelayRacePlugin plugin, String title, String color) {
        NamespacedKey key = new NamespacedKey(plugin, "remaining_time");
        BarColor barColor = parseColor(color);
        return new LatestBossBarHandle(
            Bukkit.createBossBar(key, title, barColor, BarStyle.SEGMENTED_10));
    }

    private Component deserialize(String message) {
        return message == null || message.isEmpty()
            ? Component.empty()
            : this.miniMessage.deserialize(message);
    }

    private static BarColor parseColor(String color) {
        try {
            return BarColor.valueOf(color);
        } catch (IllegalArgumentException e) {
            return BarColor.GREEN;
        }
    }
}