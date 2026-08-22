package top.syshub.relayrace.latest;

import org.bukkit.Bukkit;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import top.syshub.relayrace.common.RelayRacePlugin;
import top.syshub.relayrace.common.api.CancellableTask;
import top.syshub.relayrace.common.api.Scheduler;

public final class LatestScheduler implements Scheduler {

    @Override
    public CancellableTask runAtFixedRate(RelayRacePlugin plugin, Runnable task,
                                          long delayTicks, long periodTicks) {
        ScheduledTask scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
            plugin, ignored -> task.run(), delayTicks, periodTicks);
        return scheduledTask::cancel;
    }

    @Override
    public void runDelayed(RelayRacePlugin plugin, Runnable task, long delayTicks) {
        Bukkit.getGlobalRegionScheduler().runDelayed(
            plugin, ignored -> task.run(), delayTicks);
    }

    @Override
    public void runAsync(RelayRacePlugin plugin, Runnable task) {
        Bukkit.getAsyncScheduler().runNow(
            plugin, ignored -> task.run());
    }
}
