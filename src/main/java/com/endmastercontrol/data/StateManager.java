package com.endmastercontrol.data;

import com.endmastercontrol.EndMasterControlPlugin;
import com.endmastercontrol.config.ConfigManager;
import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class StateManager {

    private final EndMasterControlPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    private boolean endLocked;
    private boolean dragonEnabled;

    public StateManager(EndMasterControlPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "state.yml");
        this.data = YamlConfiguration.loadConfiguration(file);

        this.endLocked = data.getBoolean("endLocked", configManager.defaultEndLocked());
        this.dragonEnabled = data.getBoolean("dragonEnabled", configManager.defaultDragonEnabled());
        save();
    }

    public boolean isEndLocked() {
        return endLocked;
    }

    public void setEndLocked(boolean endLocked) {
        this.endLocked = endLocked;
        save();
    }

    public boolean isDragonEnabled() {
        return dragonEnabled;
    }

    public void setDragonEnabled(boolean dragonEnabled) {
        this.dragonEnabled = dragonEnabled;
        save();
    }

    public void save() {
        data.set("endLocked", endLocked);
        data.set("dragonEnabled", dragonEnabled);
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder.");
            }
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save state.yml: " + exception.getMessage());
        }
    }
}
