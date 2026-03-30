package com.endmastercontrol.config;

import com.endmastercontrol.EndMasterControlPlugin;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigManager {

    private final EndMasterControlPlugin plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    public ConfigManager(EndMasterControlPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public boolean defaultEndLocked() {
        return plugin.getConfig().getBoolean("defaultEndLocked", false);
    }

    public boolean defaultDragonEnabled() {
        return plugin.getConfig().getBoolean("defaultDragonEnabled", false);
    }

    public boolean disableDragonMovement() {
        return plugin.getConfig().getBoolean("disableDragonMovement", false);
    }

    public Component message(String path) {
        String raw = plugin.getConfig().getString("messages." + path, "");
        return serializer.deserialize(Objects.requireNonNullElse(raw, ""));
    }

    public String text(String path) {
        return plugin.getConfig().getString("messages." + path, "");
    }

    public Component parse(String text) {
        return serializer.deserialize(text);
    }

    public Location resolveTeleportLocation() {
        FileConfiguration config = plugin.getConfig();
        boolean useWorldSpawn = config.getBoolean("teleportLocation.useWorldSpawn", true);

        World world;
        Location base;
        if (useWorldSpawn) {
            world = plugin.getServer().getWorlds().stream()
                .filter(current -> current.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElse(plugin.getServer().getWorlds().isEmpty() ? null : plugin.getServer().getWorlds().get(0));
            if (world == null) {
                throw new IllegalStateException("No loaded worlds available for teleport location");
            }
            base = world.getSpawnLocation();
        } else {
            String worldName = config.getString("teleportLocation.world", "world");
            world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                world = plugin.getServer().getWorlds().stream()
                    .filter(current -> current.getEnvironment() == World.Environment.NORMAL)
                    .findFirst()
                    .orElse(plugin.getServer().getWorlds().isEmpty() ? null : plugin.getServer().getWorlds().get(0));
            }
            if (world == null) {
                throw new IllegalStateException("No loaded worlds available for teleport location");
            }
            base = new Location(
                world,
                config.getDouble("teleportLocation.x", 0.5D),
                config.getDouble("teleportLocation.y", 100.0D),
                config.getDouble("teleportLocation.z", 0.5D),
                (float) config.getDouble("teleportLocation.yaw", 0.0D),
                (float) config.getDouble("teleportLocation.pitch", 0.0D)
            );
        }

        return findSafeLocation(base);
    }

    private Location findSafeLocation(Location base) {
        World world = Objects.requireNonNull(base.getWorld(), "Teleport location world cannot be null");
        int x = base.getBlockX();
        int z = base.getBlockZ();
        int startY = Math.max(world.getMinHeight() + 1, Math.min(base.getBlockY(), world.getMaxHeight() - 2));

        for (int y = startY; y < world.getMaxHeight() - 1; y++) {
            if (isSafe(world, x, y, z)) {
                return centered(world, x, y, z, base.getYaw(), base.getPitch());
            }
        }

        int highest = Math.max(world.getMinHeight() + 1, world.getHighestBlockYAt(x, z) + 1);
        for (int y = highest; y < world.getMaxHeight() - 1; y++) {
            if (isSafe(world, x, y, z)) {
                return centered(world, x, y, z, base.getYaw(), base.getPitch());
            }
        }

        return new Location(world, x + 0.5D, highest, z + 0.5D, base.getYaw(), base.getPitch());
    }

    private boolean isSafe(World world, int x, int y, int z) {
        return world.getBlockAt(x, y - 1, z).getType().isSolid()
            && world.getBlockAt(x, y, z).isPassable()
            && world.getBlockAt(x, y + 1, z).isPassable();
    }

    private Location centered(World world, int x, int y, int z, float yaw, float pitch) {
        return new Location(world, x + 0.5D, y, z + 0.5D, yaw, pitch);
    }
}
