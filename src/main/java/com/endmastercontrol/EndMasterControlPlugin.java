package com.endmastercontrol;

import com.endmastercontrol.command.EndCommand;
import com.endmastercontrol.config.ConfigManager;
import com.endmastercontrol.control.BossBarManager;
import com.endmastercontrol.control.EndControlListener;
import com.endmastercontrol.control.EndController;
import com.endmastercontrol.control.TimerManager;
import com.endmastercontrol.data.StateManager;
import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EndMasterControlPlugin extends JavaPlugin {

    private StateManager stateManager;
    private BossBarManager bossBarManager;
    private TimerManager timerManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ConfigManager configManager = new ConfigManager(this);
        this.stateManager = new StateManager(this, configManager);
        this.bossBarManager = new BossBarManager();
        this.timerManager = new TimerManager(this);

        EndController endController = new EndController(this, configManager, stateManager, bossBarManager, timerManager);
        PluginCommand command = Objects.requireNonNull(getCommand("end"), "Command /end must be defined");
        EndCommand endCommand = new EndCommand(endController, configManager, timerManager);
        command.setExecutor(endCommand);
        command.setTabCompleter(endCommand);

        getServer().getPluginManager().registerEvents(new EndControlListener(endController, configManager, bossBarManager), this);

        endController.start();
        getLogger().info("EndMasterControl enabled.");
    }

    @Override
    public void onDisable() {
        if (timerManager != null) {
            timerManager.cancelActiveTimer(false);
        }
        if (bossBarManager != null) {
            bossBarManager.shutdown();
        }
        if (stateManager != null) {
            stateManager.save();
        }
    }
}
