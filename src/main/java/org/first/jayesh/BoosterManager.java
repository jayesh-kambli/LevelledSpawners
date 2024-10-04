package org.first.jayesh;

import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BoosterManager implements Listener {

    private final Sigma plugin;
    private final Connection connection;
    private final FileConfiguration config;
    private final Economy economy;

    public BoosterManager(Sigma plugin, Connection connection, FileConfiguration config, Economy economy) {
        this.plugin = plugin;
        this.connection = connection;
        this.config = config;
        this.economy = economy;
    }

    public void startScheduler() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAndExpireBoosters();
            }
        }.runTaskTimer(plugin, 0, 20 * plugin.getConfig().getInt("iteration-time")); // sec
    }

    public String custMessage(String arg) {
        @NotNull FileConfiguration config = plugin.getConfig();
        MessageLoader msgLoader = new MessageLoader(config.getString("lang"));
        return msgLoader.getMessage(arg);
    }

    private void checkAndExpireBoosters() {
        long currentTimestamp = System.currentTimeMillis() / 1000; // Current time in seconds
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM boosters WHERE endTime <= ?")) {
            statement.setLong(1, currentTimestamp);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                // Retrieve booster information
                String world = resultSet.getString("world");
                int x = resultSet.getInt("x");
                int y = resultSet.getInt("y");
                int z = resultSet.getInt("z");
                String playerName = resultSet.getString("playerName");

                //restore spawner
                restoreSpawnerValues(world, x, y, z);

                // Notify player (replace with your desired action)
                Player player = plugin.getServer().getPlayer(resultSet.getString("playerName"));
                if (player != null) {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + custMessage("UpdateAfterBoostIsExpired"));
                }

            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking and expiring boosters: " + e.getMessage());
        }
    }

    public void addBooster(String world, int x, int y, int z, long durationSeconds, String playerName) {
        if (isBoosterActive(world, x, y, z)) {
            // Booster is already active at this location
            Player player = plugin.getServer().getPlayer(playerName);
            if (player != null) {
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD +  custMessage("Aboosterisalreadyactive"));
            }
            return;
        }

        long currentTimestamp = System.currentTimeMillis() / 1000; // Current time in seconds
        long endTime = currentTimestamp + durationSeconds;

        try {
            // Save original spawner data
            Location spawnerLocation = new Location(Bukkit.getWorld(world), x, y, z);
            Block spawnerBlock = spawnerLocation.getBlock();

            if (spawnerBlock.getType() == Material.SPAWNER) {
                Player player = plugin.getServer().getPlayer(playerName);

//                if(handleTransaction(player, config.getInt("upgrade_levels.1000.money"))) {
                if (economy.getBalance(player) >= config.getInt("upgrade_levels.1000.money")) {
                    CreatureSpawner spawner = (CreatureSpawner) spawnerBlock.getState();
                    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO boosters (world, x, y, z, startTime, endTime, playerName) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                        statement.setString(1, world);
                        statement.setInt(2, x);
                        statement.setInt(3, y);
                        statement.setInt(4, z);
                        statement.setLong(5, currentTimestamp);
                        statement.setLong(6, endTime);
                        statement.setString(7, playerName);
                        statement.executeUpdate();

                        // Insert original spawner data into booster_backup table
                        insertBoosterBackup(playerName, spawnerLocation, spawner);

                        // Execute /ccheck command
//                        Player player = plugin.getServer().getPlayer(playerName);
                        if (player != null) {
                            plugin.getCheckBlockCommand().onCommand(player, null, "ccheck", new String[]{"1000"});
//                        player.sendMessage(ChatColor.GREEN + "Spawner boosted for " + (durationSeconds / 60) + " minutes!");

                        }
                    }
                } else {
                    player.sendMessage( ChatColor.RED + "" + ChatColor.BOLD + custMessage("noMoney"));
                }

            } else {
                plugin.getLogger().warning("Tried to add booster to non-spawner block.");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error adding booster: " + e.getMessage());
        }
    }

    public boolean handleTransaction(Player player, double amount) {
        if (economy.getBalance(player) >= amount) {
            return true;
        } else {
            return false;
        }
    }

    private void insertBoosterBackup(String playerName, Location location, CreatureSpawner spawner) {
        String locationString = locationToString(location);

        Player player = plugin.getServer().getPlayer(playerName);
        if(player != null) {
            Block seeSpawner = player.getTargetBlockExact(5);
            try {
                // Check if the location already exists in booster_backup
                if (!isLocationStored(locationString, "booster_backup")) {
                    int levelInfo = getSpawnerLevel(seeSpawner);
                    if(levelInfo != 0) {
                        // Location is not stored, insert the data
                        String insert = "INSERT INTO booster_backup (world, x, y, z, playerName, spawnerType, spawnDelay, minSpawnDelay, maxSpawnDelay, spawnCount, maxNearbyEntities, requiredPlayerRange, level) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement ps = connection.prepareStatement(insert)) {
                            ps.setString(1, location.getWorld().getName());
                            ps.setInt(2, location.getBlockX());
                            ps.setInt(3, location.getBlockY());
                            ps.setInt(4, location.getBlockZ());
                            ps.setString(5, playerName);
                            ps.setString(6, spawner.getSpawnedType().name());
                            ps.setInt(7, spawner.getDelay());
                            ps.setInt(8, spawner.getMinSpawnDelay());
                            ps.setInt(9, spawner.getMaxSpawnDelay());
                            ps.setInt(10, spawner.getSpawnCount());
                            ps.setInt(11, spawner.getMaxNearbyEntities());
                            ps.setInt(12, spawner.getRequiredPlayerRange());
                            ps.setInt(13, levelInfo);
                            ps.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Error saving original spawner data to booster_backup: " + e.getMessage());
            }
        }
    }

    private String locationToString(Location location) {
        if (location == null) {
            return null;
        }
        return String.format("%s,%d,%d,%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    private String BlockToString(Block block) {
        if (block == null) {
            return null;
        }
        return String.format("%s,%d,%d,%d",
                block.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ());
    }


    private boolean isLocationStored(String locationString, String table) {
        String query = "SELECT COUNT(*) FROM " + table + " WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            String[] parts = locationString.split(",");
            ps.setString(1, parts[0]);
            ps.setInt(2, Integer.parseInt(parts[1]));
            ps.setInt(3, Integer.parseInt(parts[2]));
            ps.setInt(4, Integer.parseInt(parts[3]));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
            }
        } catch (SQLException | NumberFormatException e) {
            plugin.getLogger().warning("Error checking stored location: " + e.getMessage());
        }
        return false;
    }

    private int getSpawnerLevel(Block block) {
        if (block == null || block.getType() != Material.SPAWNER) {
            return 0; // Return 0 if the block is not a spawner or is null
        }

        String locationString = BlockToString(block);

        try {
            String selectLevel = "SELECT level FROM spawners WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement ps = connection.prepareStatement(selectLevel)) {
                String[] parts = locationString.split(",");
                ps.setString(1, parts[0]);
                ps.setInt(2, Integer.parseInt(parts[1]));
                ps.setInt(3, Integer.parseInt(parts[2]));
                ps.setInt(4, Integer.parseInt(parts[3]));

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("level");
                    } else {
                        return 1; // Return 0 if the spawner does not exist in the table
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error retrieving spawner level: " + e.getMessage());
        }

        return 0; // Return 0 in case of any exception or error
    }

    // New end ===============================================================================>

    private void deleteBoosterRecord(String world, int x, int y, int z) { //good
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM boosters WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error deleting booster record: " + e.getMessage());
        }
    }

    private void deleteOriginalSpawnerRecord(String world, int x, int y, int z) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM booster_backup WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error deleting original spawner record: " + e.getMessage());
        }
    }

    private void deleteSpawnerRecord(String world, int x, int y, int z) { //no need to delete - edit
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM original_spawners WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            statement.executeUpdate();

            try (PreparedStatement statement2 = connection.prepareStatement("DELETE FROM spawners WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                statement2.setString(1, world);
                statement2.setInt(2, x);
                statement2.setInt(3, y);
                statement2.setInt(4, z);
                statement2.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Error deleting spawner record: " + e.getMessage());
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Error deleting spawner record: " + e.getMessage());
        }
    }


    public void restoreSpawnerValues(String world, int x, int y, int z) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM booster_backup WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int spawnDelay = resultSet.getInt("spawnDelay");
                int minSpawnDelay = resultSet.getInt("minSpawnDelay");
                int maxSpawnDelay = resultSet.getInt("maxSpawnDelay");
                int spawnCount = resultSet.getInt("spawnCount");
                int maxNearbyEntities = resultSet.getInt("maxNearbyEntities");
                int requiredPlayerRange = resultSet.getInt("requiredPlayerRange");
                String playerName = resultSet.getString("playerName");
                String spawnerType = resultSet.getString("spawnerType");
                int level = resultSet.getInt("level");

                // Get the spawner at the specified location
                Location targetLocation = new Location(plugin.getServer().getWorld(world), x, y, z);
                if (targetLocation.getBlock().getState() instanceof CreatureSpawner) {
                    CreatureSpawner spawner = (CreatureSpawner) targetLocation.getBlock().getState();
                    if (spawner.getMaxSpawnDelay() > maxSpawnDelay) {
                        spawner.setMinSpawnDelay(minSpawnDelay);
                        spawner.setMaxSpawnDelay(maxSpawnDelay);
                    } else {
                        spawner.setMaxSpawnDelay(maxSpawnDelay);
                        spawner.setMinSpawnDelay(minSpawnDelay);
                    }

                    spawner.setDelay(spawnDelay);
                    spawner.setSpawnCount(spawnCount);
                    spawner.setMaxNearbyEntities(maxNearbyEntities);
                    spawner.setRequiredPlayerRange(requiredPlayerRange);
                    spawner.update();
                }

                if(level>1) {
                    String updateQuery = "UPDATE spawners SET playerName = ?, spawnerType = ?, spawnDelay = ?, minSpawnDelay = ?, maxSpawnDelay = ?, spawnCount = ?, maxNearbyEntities = ?, requiredPlayerRange = ?, level = ? WHERE world = ? AND x = ? AND y = ? AND z = ?";
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                        updateStatement.setString(1, playerName);
                        updateStatement.setString(2, spawnerType);
                        updateStatement.setInt(3, spawnDelay);
                        updateStatement.setInt(4, minSpawnDelay);
                        updateStatement.setInt(5, maxSpawnDelay);
                        updateStatement.setInt(6, spawnCount);
                        updateStatement.setInt(7, maxNearbyEntities);
                        updateStatement.setInt(8, requiredPlayerRange);
                        updateStatement.setInt(9, level);
                        updateStatement.setString(10, world);
                        updateStatement.setInt(11, x);
                        updateStatement.setInt(12, y);
                        updateStatement.setInt(13, z);
                        updateStatement.executeUpdate();
                    }
                } else {
                    deleteSpawnerRecord(world, x, y, z);
                }

                deleteBoosterRecord(world, x, y, z);
                deleteOriginalSpawnerRecord(world, x, y, z);

            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error restoring spawner values: " + e.getMessage());
        }
    }

    private boolean isBoosterActive(String world, int x, int y, int z) {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM boosters WHERE world = ? AND x = ? AND y = ? AND z = ? AND endTime > ?")) {
            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            statement.setLong(5, currentTimestamp);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking booster status: " + e.getMessage());
            return false;
        }
    }
}
