package org.first.jayesh;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RestoreAllSpawnerData implements CommandExecutor {

    private final JavaPlugin plugin; // Plugin instance for logging
    private final Connection connection; // Database connection

    public RestoreAllSpawnerData(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) {
            // Check if the command sender is not a player
            restoreAllSpawnerData();
            commandSender.sendMessage(ChatColor.GREEN + custMessage("SuccessfullyrestoredAll"));
            return true;
        } else {
            Player player = (Player) commandSender;
            if (player.isOp()) {
                // Check if the player is an operator (OP)
                restoreAllSpawnerData();
                player.sendMessage(ChatColor.GREEN + custMessage("SuccessfullyrestoredAll"));
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return false;
            }
        }
    }

    public String custMessage(String arg) {
        @NotNull FileConfiguration config = plugin.getConfig();
        MessageLoader msgLoader = new MessageLoader(config.getString("lang"));
        return msgLoader.getMessage(arg);
    }

    private void restoreAllSpawnerData() {
        try {
            String selectAll = "SELECT * FROM original_spawners";
            try (PreparedStatement ps = connection.prepareStatement(selectAll)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        restoreSpawnerData(rs);
                    }
                }
            }

            clearDatabase();

        } catch (SQLException e) {
            plugin.getLogger().warning("Error restoring all spawner data: " + e.getMessage());
        }
    }

    private void restoreSpawnerData(ResultSet rs) throws SQLException {
        String world = rs.getString("world");
        int x = rs.getInt("x");
        int y = rs.getInt("y");
        int z = rs.getInt("z");
        String playerName = rs.getString("playerName");
        String spawnerType = rs.getString("spawnerType");
        int spawnDelay = rs.getInt("spawnDelay");
        int minSpawnDelay = rs.getInt("minSpawnDelay");
        int maxSpawnDelay = rs.getInt("maxSpawnDelay");
        int spawnCount = rs.getInt("spawnCount");
        int maxNearbyEntities = rs.getInt("maxNearbyEntities");
        int requiredPlayerRange = rs.getInt("requiredPlayerRange");

        Location spawnerLocation = new Location(Bukkit.getWorld(world), x, y, z);
        Block block = spawnerLocation.getBlock();

        if (block.getState() instanceof CreatureSpawner) {
            CreatureSpawner spawner = (CreatureSpawner) block.getState();

            if (spawner.getMaxSpawnDelay() < maxSpawnDelay) {
                spawner.setMaxSpawnDelay(maxSpawnDelay);
                spawner.setMinSpawnDelay(minSpawnDelay);
            } else {
                spawner.setMinSpawnDelay(minSpawnDelay);
                spawner.setMaxSpawnDelay(maxSpawnDelay);
            }
            spawner.setSpawnedType(EntityType.valueOf(spawnerType));
            spawner.setDelay(spawnDelay);
//            spawner.setMinSpawnDelay(minSpawnDelay);
//            spawner.setMaxSpawnDelay(maxSpawnDelay);
            spawner.setSpawnCount(spawnCount);
            spawner.setMaxNearbyEntities(maxNearbyEntities);
            spawner.setRequiredPlayerRange(requiredPlayerRange);
            spawner.update();

            plugin.getLogger().info("Restored spawner data at " + spawnerLocation.toString());
        } else {
            plugin.getLogger().warning("Block at " + spawnerLocation.toString() + " is not a spawner.");
        }
    }

    private void clearDatabase() {
        clearDatabase1();
        clearDatabase2();
        clearDatabase3();
        clearDatabase4();
    }

    private void clearDatabase1() {
        try {
            String clearOriginalSpawners = "DELETE FROM original_spawners";
            try (PreparedStatement ps = connection.prepareStatement(clearOriginalSpawners)) {
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Error clearing Backup database: " + e.getMessage());
        }
    }

    private void clearDatabase2() {
        try {
            String clearOriginalSpawners = "DELETE FROM spawners";
            try (PreparedStatement ps = connection.prepareStatement(clearOriginalSpawners)) {
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Error clearing Modded database: " + e.getMessage());
        }
    }

    private void clearDatabase3() {
        try {
            String clearOriginalSpawners = "DELETE FROM boosters";
            try (PreparedStatement ps = connection.prepareStatement(clearOriginalSpawners)) {
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Error clearing Booster database: " + e.getMessage());
        }
    }

    private void clearDatabase4() {
        try {
            String clearOriginalSpawners = "DELETE FROM booster_backup";
            try (PreparedStatement ps = connection.prepareStatement(clearOriginalSpawners)) {
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Error clearing Booster Backup database: " + e.getMessage());
        }
    }
}
