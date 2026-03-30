package com.endmastercontrol.control;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public final class BossBarManager {

    private static final String DRAGON_OFF_TEXT = "Dragon Peace Mode 🕊️";
    private static final String DRAGON_ON_TEXT = "Dragon Fight Mode ⚔️";

    private final BossBar bossBar;
    private final Set<Player> trackedPlayers = new HashSet<>();

    public BossBarManager() {
        this.bossBar = Bukkit.createBossBar(DRAGON_OFF_TEXT, BarColor.BLUE, BarStyle.SOLID);
        this.bossBar.setVisible(true);
    }

    public void updateState(boolean dragonEnabled) {
        bossBar.setTitle(dragonEnabled ? DRAGON_ON_TEXT : DRAGON_OFF_TEXT);
        bossBar.setColor(dragonEnabled ? BarColor.RED : BarColor.BLUE);
        bossBar.setProgress(1.0D);
    }

    public void refreshPlayer(Player player) {
        if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
            if (trackedPlayers.add(player)) {
                bossBar.addPlayer(player);
            }
            return;
        }
        removePlayer(player);
    }

    public void removePlayer(Player player) {
        if (trackedPlayers.remove(player)) {
            bossBar.removePlayer(player);
        }
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayer(player);
        }
    }

    public void shutdown() {
        bossBar.removeAll();
        trackedPlayers.clear();
    }
}
