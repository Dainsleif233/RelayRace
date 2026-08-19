package top.syshub.relayrace.classic;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import top.syshub.relayrace.common.RelayRacePlugin;
import top.syshub.relayrace.common.api.CancellableTask;
import top.syshub.relayrace.common.api.Scheduler;

public final class ClassicScheduler implements Scheduler {

    @Override
    public CancellableTask runAtFixedRate(RelayRacePlugin plugin, Runnable task,
                                          long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        return new CancellableTask() {
            @Override
            public void cancel() {
                bukkitTask.cancel();
            }
        };
    }

    @Override
    public void runDelayed(RelayRacePlugin plugin, Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public void runAsync(RelayRacePlugin plugin, Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }
}