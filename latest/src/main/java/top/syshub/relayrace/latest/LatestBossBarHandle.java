package top.syshub.relayrace.latest;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import top.syshub.relayrace.common.api.BossBarHandle;

public final class LatestBossBarHandle implements BossBarHandle {

    private final BossBar bar;

    public LatestBossBarHandle(BossBar bar) {
        this.bar = bar;
    }

    @Override
    public void setTitle(String title) {
        bar.setTitle(title);
    }

    @Override
    public void setProgress(double progress) {
        bar.setProgress(progress);
    }

    @Override
    public void setColor(String color) {
        try {
            bar.setColor(BarColor.valueOf(color));
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    public void setVisible(boolean visible) {
        bar.setVisible(visible);
    }

    @Override
    public void addPlayer(Player player) {
        bar.addPlayer(player);
    }

    @Override
    public void removePlayer(Player player) {
        bar.removePlayer(player);
    }

    @Override
    public void removeAll() {
        bar.removeAll();
    }
}
