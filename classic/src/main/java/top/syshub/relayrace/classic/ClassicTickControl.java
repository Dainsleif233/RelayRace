package top.syshub.relayrace.classic;

import top.syshub.relayrace.common.api.TickControl;

/**
 * Paper 1.16.1 has no ServerTickManager. Tick-rate and server-freeze features
 * are intentionally disabled on this platform.
 */
public final class ClassicTickControl implements TickControl {

    @Override
    public void slowTo(float ticksPerSecond) {
    }

    @Override
    public void restore() {
    }

    @Override
    public void setFrozen(boolean frozen) {
    }

    @Override
    public boolean isFrozen() {
        return false;
    }
}