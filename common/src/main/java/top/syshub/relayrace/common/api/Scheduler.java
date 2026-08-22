package top.syshub.relayrace.common.api;

import top.syshub.relayrace.common.RelayRacePlugin;

public interface Scheduler {

    CancellableTask runAtFixedRate(RelayRacePlugin plugin, Runnable task,
                                   long delayTicks, long periodTicks);

    void runDelayed(RelayRacePlugin plugin, Runnable task, long delayTicks);

    void runAsync(RelayRacePlugin plugin, Runnable task);
}
