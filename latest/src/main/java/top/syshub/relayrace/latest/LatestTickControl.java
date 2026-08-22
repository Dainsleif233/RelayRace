package top.syshub.relayrace.latest;

import org.bukkit.Bukkit;

import top.syshub.relayrace.common.api.TickControl;

public final class LatestTickControl implements TickControl {

    private float savedTickRate = 20.0f;
    private boolean hasSavedTickRate = false;

    @Override
    public void slowTo(float ticksPerSecond) {
        if (!hasSavedTickRate) {
            savedTickRate = Bukkit.getServerTickManager().getTickRate();
            hasSavedTickRate = true;
        }
        Bukkit.getServerTickManager().setTickRate(ticksPerSecond);
    }

    @Override
    public void restore() {
        if (hasSavedTickRate) {
            Bukkit.getServerTickManager().setTickRate(savedTickRate);
            hasSavedTickRate = false;
        }
    }

    @Override
    public void setFrozen(boolean frozen) {
        Bukkit.getServerTickManager().setFrozen(frozen);
    }

    @Override
    public boolean isFrozen() {
        return Bukkit.getServerTickManager().isFrozen();
    }
}
