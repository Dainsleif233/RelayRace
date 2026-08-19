package top.syshub.relayrace.common.api;

import org.bukkit.entity.Player;

public interface BossBarHandle {

    void setTitle(String title);

    void setProgress(double progress);

    void setColor(String color);

    void setVisible(boolean visible);

    void addPlayer(Player player);

    void removePlayer(Player player);

    void removeAll();
}