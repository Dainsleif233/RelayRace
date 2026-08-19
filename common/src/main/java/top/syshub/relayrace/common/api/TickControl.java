package top.syshub.relayrace.common.api;

public interface TickControl {

    void slowTo(float ticksPerSecond);

    void restore();

    void setFrozen(boolean frozen);

    boolean isFrozen();
}