package com.endmastercontrol.control;

import com.endmastercontrol.EndMasterControlPlugin;
import java.util.Objects;
import org.bukkit.scheduler.BukkitTask;

public final class TimerManager {

    public enum TimerAction {
        UNLOCK_END("unlock the End"),
        ENABLE_DRAGON("enable the dragon fight");

        private final String description;

        TimerAction(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }
    }

    private final EndMasterControlPlugin plugin;
    private BukkitTask activeTask;
    private TimerAction activeAction;
    private long completeAtMillis;

    public TimerManager(EndMasterControlPlugin plugin) {
        this.plugin = plugin;
    }

    public void schedule(TimerAction action, int minutes, Runnable runnable) {
        cancelActiveTimer(false);
        long delayTicks = minutes * 60L * 20L;
        this.activeAction = Objects.requireNonNull(action, "action");
        this.completeAtMillis = System.currentTimeMillis() + (minutes * 60_000L);
        this.activeTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            clearTimerState();
            runnable.run();
        }, delayTicks);
    }

    public boolean hasActiveTimer() {
        return activeTask != null;
    }

    public TimerAction getActiveAction() {
        return activeAction;
    }

    public long getRemainingSeconds() {
        return Math.max(0L, (completeAtMillis - System.currentTimeMillis()) / 1000L);
    }

    public void cancelActiveTimer(boolean log) {
        if (activeTask != null) {
            activeTask.cancel();
            clearTimerState();
            if (log) {
                plugin.getLogger().info("Cancelled active End timer.");
            }
        }
    }

    private void clearTimerState() {
        this.activeTask = null;
        this.activeAction = null;
        this.completeAtMillis = 0L;
    }
}
