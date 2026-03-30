package com.endmastercontrol.control;

import com.endmastercontrol.config.ConfigManager;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class EndControlListener implements Listener {

    private final EndController endController;
    private final ConfigManager configManager;
    private final BossBarManager bossBarManager;

    public EndControlListener(EndController endController, ConfigManager configManager, BossBarManager bossBarManager) {
        this.endController = endController;
        this.configManager = configManager;
        this.bossBarManager = bossBarManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (endController.shouldBlockEndAccess(event.getTo())) {
            event.setCancelled(true);
            endController.handleBlockedTeleport(event.getPlayer());
            return;
        }

        endController.refreshBossBar(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (endController.shouldBlockEndAccess(event.getTo())) {
            event.setCancelled(true);
            endController.handleBlockedTeleport(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (endController.isEndLocked() && endController.isEndWorld(player.getWorld())) {
            endController.teleportPlayerOut(player, configManager.message("endLocked"));
            return;
        }
        bossBarManager.refreshPlayer(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        bossBarManager.refreshPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        bossBarManager.removePlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof EnderDragon dragon) || endController.isDragonEnabled()) {
            return;
        }

        double finalHealth = dragon.getHealth() - event.getFinalDamage();
        event.setCancelled(true);
        endController.markDragonAnchor(dragon);
        if (finalHealth <= 0.0D) {
            endController.preventDragonDeath(dragon);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (endController.isDragonEnabled()) {
            return;
        }

        if (event.getDamager() instanceof EnderDragon) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDragonHeal(EntityRegainHealthEvent event) {
        if (!endController.isDragonEnabled() && event.getEntity() instanceof EnderDragon) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            endController.clearDragonAnchor(event.getEntity());
        }
    }
}
