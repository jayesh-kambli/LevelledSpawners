package org.first.jayesh;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class restore implements CommandExecutor {

    private final JavaPlugin plugin; // Field to store the plugin instance
    private final Connection connection; // Database connection

    private int restoredMinSpawnDelay;
    private int restoredMaxSpawnDelay;

    public restore(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;

                Location targetLocation = player.getTargetBlockExact(5).getLocation();

                String locationString = locationToString(targetLocation);

                try {
                    String selectOriginal = "SELECT * FROM original_spawners WHERE world = ? AND x = ? AND y = ? AND z = ?";
                    try (PreparedStatement ps = connection.prepareStatement(selectOriginal)) {
                        String[] parts = locationString.split(",");
                        ps.setString(1, parts[0]);
                        ps.setInt(2, Integer.parseInt(parts[1]));
                        ps.setInt(3, Integer.parseInt(parts[2]));
                        ps.setInt(4, Integer.parseInt(parts[3]));

                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                // Restore original data
                                restoreSpawnerData(rs, targetLocation);
                                deleteEntries(locationString);
                                deleteOrgEntries(locationString);
                                player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + custMessage("Restored"));
//                                player.sendMessage("Restored original data for the spawner at " + locationString);
                            } else {
                                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + custMessage("Nodatabackup"));
//                                player.sendMessage("No original data found for the spawner at " + locationString);
                            }
                        }
                    }
                } catch (SQLException e) {
                    plugin.getLogger().warning("Error restoring spawner data: " + e.getMessage());
                }

            return true;
        } else {
            commandSender.sendMessage("Only players can execute this command.");
            return false;
        }
    }

    public String custMessage(String arg) {
        @NotNull FileConfiguration config = plugin.getConfig();
        MessageLoader msgLoader = new MessageLoader(config.getString("lang"));
        return msgLoader.getMessage(arg);
    }

    private void restoreSpawnerData(ResultSet rs, Location targetLocation) throws SQLException {
        String playerName = rs.getString("playerName");
        String spawnerType = rs.getString("spawnerType");
        int spawnDelay = rs.getInt("spawnDelay");
        int minSpawnDelay = rs.getInt("minSpawnDelay");
        int maxSpawnDelay = rs.getInt("maxSpawnDelay");
        int spawnCount = rs.getInt("spawnCount");
        int maxNearbyEntities = rs.getInt("maxNearbyEntities");
        int requiredPlayerRange = rs.getInt("requiredPlayerRange");

        // Store min and max spawn delay for further operations
        restoredMinSpawnDelay = minSpawnDelay;
        restoredMaxSpawnDelay = maxSpawnDelay;

        // Find the spawner block at the target location
        if (targetLocation.getBlock().getType().toString().contains("SPAWNER")) {
            CreatureSpawner spawner = (CreatureSpawner) targetLocation.getBlock().getState();

            // Update spawn delay
            if (spawner.getMaxSpawnDelay() < restoredMaxSpawnDelay) {
                spawner.setMaxSpawnDelay(restoredMaxSpawnDelay);
                spawner.setMinSpawnDelay(restoredMinSpawnDelay);
            } else {
                spawner.setMinSpawnDelay(restoredMinSpawnDelay);
                spawner.setMaxSpawnDelay(restoredMaxSpawnDelay);
            }

            // Set spawner properties
            spawner.setSpawnedType(EntityType.valueOf(spawnerType));
            spawner.setDelay(spawnDelay);
            spawner.setSpawnCount(spawnCount);
            spawner.setMaxNearbyEntities(maxNearbyEntities);
            spawner.setRequiredPlayerRange(requiredPlayerRange);



            spawner.update(); // Update the spawner in-game
        } else {
            plugin.getLogger().warning("No spawner found at the target location.");
        }
    }

    private void deleteEntries(String locationString) {
        try {
            String deleteSpawners = "DELETE FROM spawners WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement ps = connection.prepareStatement(deleteSpawners)) {
                String[] parts = locationString.split(",");
                ps.setString(1, parts[0]);
                ps.setInt(2, Integer.parseInt(parts[1]));
                ps.setInt(3, Integer.parseInt(parts[2]));
                ps.setInt(4, Integer.parseInt(parts[3]));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error deleting entries from spawners table: " + e.getMessage());
        }
    }

    private void deleteOrgEntries(String locationString) {
        try {
            String deleteSpawners = "DELETE FROM original_spawners WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement ps = connection.prepareStatement(deleteSpawners)) {
                String[] parts = locationString.split(",");
                ps.setString(1, parts[0]);
                ps.setInt(2, Integer.parseInt(parts[1]));
                ps.setInt(3, Integer.parseInt(parts[2]));
                ps.setInt(4, Integer.parseInt(parts[3]));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error deleting entries from Org spawners table: " + e.getMessage());
        }
    }

    private String locationToString(Location location) {
        return String.format("%s,%d,%d,%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }
}
