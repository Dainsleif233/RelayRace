package top.syshub.relayrace.classic;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import top.syshub.relayrace.common.RelayRacePlugin;
import top.syshub.relayrace.common.api.BossBarHandle;
import top.syshub.relayrace.common.api.PlayerUi;

public final class ClassicPlayerUi implements PlayerUi {

    @Override
    public void setPlayerListName(Player player, String miniMessage) {
        player.setPlayerListName(ClassicText.toLegacy(miniMessage));
    }

    @Override
    public void setPlayerDisplayName(Player player, String miniMessage) {
        player.setDisplayName(ClassicText.toLegacy(miniMessage));
    }

    @Override
    public void clearDisplayName(Player player) {
        player.setPlayerListName(null);
        player.setDisplayName(null);
    }

    @Override
    public void sendMessage(CommandSender sender, String miniMessage) {
        sender.sendMessage(ClassicText.toLegacy(miniMessage));
    }

    @Override
    public void sendTitle(Player player, String titleMiniMessage, String subtitleMiniMessage,
                          int fadeInTicks, int stayTicks, int fadeOutTicks) {
        player.sendTitle(
            ClassicText.toLegacy(titleMiniMessage),
            ClassicText.toLegacy(subtitleMiniMessage),
            fadeInTicks, stayTicks, fadeOutTicks);
    }

    @Override
    public BossBarHandle createBossBar(RelayRacePlugin plugin, String title, String color) {
        return new ClassicBossBarHandle(
            Bukkit.createBossBar(ClassicText.toLegacy(title), parseColor(color), BarStyle.SEGMENTED_10));
    }

    private static BarColor parseColor(String color) {
        try {
            return BarColor.valueOf(color);
        } catch (IllegalArgumentException e) {
            return BarColor.GREEN;
        }
    }
}