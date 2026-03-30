package com.endmastercontrol.control;

import com.endmastercontrol.EndMasterControlPlugin;
import com.endmastercontrol.config.ConfigManager;
import com.endmastercontrol.control.TimerManager.TimerAction;
import com.endmastercontrol.data.StateManager;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class EndController {

    private final EndMasterControlPlugin plugin;
    private final ConfigManager configManager;
    private final StateManager stateManager;
    private final BossBarManager bossBarManager;
    private final TimerManager timerManager;
    private final Map<UUID, Location> frozenDragons = new HashMap<>();

    private BukkitTask dragonTask;

    public EndController(
        EndMasterControlPlugin plugin,
        ConfigManager configManager,
        StateManager stateManager,
        BossBarManager bossBarManager,
        TimerManager timerManager
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.stateManager = stateManager;
        this.bossBarManager = bossBarManager;
        this.timerManager = timerManager;
    }

    public void start() {
        bossBarManager.updateState(isDragonEnabled());
        bossBarManager.refreshAll();
        startDragonTask();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (isEndLocked()) {
                evacuatePlayersFromEnd(configManager.message("endLocked"));
            }
            bossBarManager.refreshAll();
        });
    }

    public boolean isEndLocked() {
        return stateManager.isEndLocked();
    }

    public boolean isDragonEnabled() {
        return stateManager.isDragonEnabled();
    }

    public boolean lockEnd(CommandSender sender) {
        if (isEndLocked()) {
            sender.sendMessage(Component.text("The End is already locked."));
            return false;
        }

        stateManager.setEndLocked(true);
        broadcast(configManager.message("endLockedBroadcast"));
        evacuatePlayersFromEnd(configManager.message("endLocked"));
        sender.sendMessage(Component.text("End access disabled."));
        return true;
    }

    public boolean unlockEnd(CommandSender sender, boolean broadcastEffects) {
        if (!isEndLocked()) {
            sender.sendMessage(Component.text("The End is already unlocked."));
            return false;
        }

        stateManager.setEndLocked(false);
        sender.sendMessage(Component.text("End access enabled."));
        if (broadcastEffects) {
            broadcast(configManager.message("endUnlocked"));
            titleAll("The End has been opened!");
            soundAll(Sound.UI_TOAST_CHALLENGE_COMPLETE);
        }
        return true;
    }

    public boolean setDragonEnabled(CommandSender sender, boolean enabled, boolean broadcastEffects) {
        if (isDragonEnabled() == enabled) {
            sender.sendMessage(Component.text(enabled ? "Dragon is already enabled." : "Dragon is already peaceful."));
            return false;
        }

        stateManager.setDragonEnabled(enabled);
        bossBarManager.updateState(enabled);
        bossBarManager.refreshAll();

        if (enabled) {
            sender.sendMessage(configManager.message("dragonOn"));
            clearFrozenDragons();
            if (broadcastEffects) {
                broadcast(configManager.message("dragonOn"));
                titleAll("Dragon Fight Enabled!");
            }
        } else {
            sender.sendMessage(configManager.message("dragonOff"));
            if (broadcastEffects) {
                broadcast(configManager.message("dragonOff"));
            }
            freezeExistingDragons();
        }
        return true;
    }

    public void scheduleTimer(CommandSender sender, int minutes) {
        TimerAction action = chooseTimerAction();
        if (action == null) {
            sender.sendMessage(configManager.message("timerNothingToDo"));
            return;
        }

        if (timerManager.hasActiveTimer()) {
            timerManager.cancelActiveTimer(false);
            sender.sendMessage(configManager.message("timerCancelled"));
        }

        timerManager.schedule(action, minutes, () -> {
            if (action == TimerAction.UNLOCK_END) {
                unlockEnd(plugin.getServer().getConsoleSender(), true);
                broadcast(Component.text("Timer complete: the End is now unlocked."));
            } else {
                setDragonEnabled(plugin.getServer().getConsoleSender(), true, true);
                broadcast(Component.text("Timer complete: the dragon fight is now enabled."));
            }
        });

        String raw = configManager.text("timerScheduled")
            .replace("%action%", action.description())
            .replace("%minutes%", Integer.toString(minutes));
        sender.sendMessage(configManager.parse(raw));
    }

    public TimerAction chooseTimerAction() {
        if (isEndLocked()) {
            return TimerAction.UNLOCK_END;
        }
        if (!isDragonEnabled()) {
            return TimerAction.ENABLE_DRAGON;
        }
        return null;
    }

    public boolean shouldBlockEndAccess(Location destination) {
        return isEndLocked() && destination != null && isEndWorld(destination.getWorld());
    }

    public void handleBlockedTeleport(Player player) {
        player.sendMessage(configManager.message("endLocked"));
    }

    public void evacuatePlayersFromEnd(Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isEndWorld(player.getWorld())) {
                teleportPlayerOut(player, message);
            }
        }
    }

    public void teleportPlayerOut(Player player, Component message) {
        Location safeLocation = configManager.resolveTeleportLocation();
        player.teleportAsync(safeLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.sendMessage(message);
        bossBarManager.removePlayer(player);
    }

    public Collection<World> getEndWorlds() {
        return plugin.getServer().getWorlds().stream()
            .filter(world -> world.getEnvironment() == World.Environment.THE_END)
            .toList();
    }

    public boolean isEndWorld(World world) {
        return world != null && world.getEnvironment() == World.Environment.THE_END;
    }

    public void refreshBossBar(Player player) {
        bossBarManager.refreshPlayer(player);
    }

    public void removeBossBar(Player player) {
        bossBarManager.removePlayer(player);
    }

    public void markDragonAnchor(EnderDragon dragon) {
        frozenDragons.computeIfAbsent(dragon.getUniqueId(), ignored -> dragon.getLocation().clone());
    }

    public void clearDragonAnchor(Entity entity) {
        frozenDragons.remove(entity.getUniqueId());
    }

    public void preventDragonDeath(EnderDragon dragon) {
        double maxHealth = Objects.requireNonNull(dragon.getAttribute(Attribute.MAX_HEALTH)).getValue();
        dragon.setHealth(Math.min(maxHealth, 1.0D));
        dragon.setPhase(EnderDragon.Phase.HOVER);
    }

    private void startDragonTask() {
        dragonTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (isDragonEnabled() || !configManager.disableDragonMovement()) {
                if (isDragonEnabled()) {
                    clearFrozenDragons();
                }
                return;
            }

            for (World world : getEndWorlds()) {
                for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
                    Location anchor = frozenDragons.computeIfAbsent(dragon.getUniqueId(), ignored -> dragon.getLocation().clone());
                    if (!Objects.equals(anchor.getWorld(), dragon.getWorld())) {
                        anchor = dragon.getLocation().clone();
                        frozenDragons.put(dragon.getUniqueId(), anchor);
                    }

                    dragon.setVelocity(new Vector(0, 0, 0));
                    dragon.setPhase(EnderDragon.Phase.HOVER);
                    if (dragon.getLocation().distanceSquared(anchor) > 2.25D) {
                        dragon.teleport(anchor);
                    }
                }
            }
        }, 1L, 5L);
    }

    private void freezeExistingDragons() {
        for (World world : getEndWorlds()) {
            for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
                markDragonAnchor(dragon);
                dragon.setVelocity(new Vector(0, 0, 0));
                dragon.setPhase(EnderDragon.Phase.HOVER);
            }
        }
    }

    private void clearFrozenDragons() {
        frozenDragons.clear();
    }

    private void broadcast(Component message) {
        plugin.getServer().broadcast(message);
    }

    private void soundAll(Sound sound) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
        }
    }

    private void titleAll(String title) {
        Title.Times times = Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(500));
        Title built = Title.title(Component.text(title), Component.empty(), times);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(built);
        }
    }
}
