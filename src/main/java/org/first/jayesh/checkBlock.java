package org.first.jayesh;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class checkBlock implements CommandExecutor {

    private final JavaPlugin plugin; // Field to store the plugin instance
    private final Connection connection; // Database connection
    private final FileConfiguration config; // Configuration file
    private final Economy economy;

    public checkBlock(JavaPlugin plugin, Connection connection, FileConfiguration config, Economy economy) {
        this.plugin = plugin;
        this.connection = connection;
        this.config = config;
        this.economy = economy;

    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (commandSender instanceof Player) {



            Player player = (Player) commandSender;
            // Get target block within 5 blocks distance
            Block targetBlock = player.getTargetBlock(null, 5);

            // Check if the target block is a spawner
            if (targetBlock.getType() == Material.SPAWNER) {
                CreatureSpawner spawner = (CreatureSpawner) targetBlock.getState();
                Location spawnerLocation = spawner.getLocation();

                // Check if arguments are provided
                if (args.length == 1) {

                    try {
                        int level = Integer.parseInt(args[0]);
                        ConfigurationSection upgradeConfig = plugin.getConfig().getConfigurationSection("upgrade_levels." + level);
                        if (upgradeConfig != null) {
                            int money = config.getInt("upgrade_levels." + level + ".money");

                            if(handleTransaction(player, money, level)) {
                                saveOriginalSpawnerData(player.getName(), spawner, spawnerLocation);

                                // Update spawner properties based on the specified level
                                updateSpawnerFromConfig(spawner, level);

                                // Update or insert into spawners table
                                insertOrUpdateSpawnerData(player.getName(), spawner, spawnerLocation, level);
                                if(level!=1000) {
                                    player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + custMessage("spawnerUpgrade")+ " " + level + custMessage("spawnerUpgrade2"));
                                }
                            }
                        } else {
                            player.sendMessage(custMessage("maxLevel"));
                        }


                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + custMessage("invalidLevel"));
                    }
                } else {
//                    player.sendMessage(ChatColor.RED +"Usage: /ccheck <level>");
                }
            } else {
                player.sendMessage(ChatColor.RED + custMessage("noSpawnerDetected"));
            }

        } else {
            commandSender.sendMessage(ChatColor.RED +"Only players can execute this command. (Error: 101)");
        }
        return true;
    }

    public String custMessage(String arg) {
        MessageLoader msgLoader = new MessageLoader(config.getString("lang"));
        return msgLoader.getMessage(arg);
    }

    public boolean handleTransaction(Player player, double amount, int level) {
        if (economy.getBalance(player) >= amount) {
            economy.withdrawPlayer(player, amount);

            if(level == 1000) {
                player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + custMessage("SpawnerBoostedSuccessfully") + " " + config.getInt("booster-time") + "s ($" +amount+  ")");
            } else {
//                player.sendMessage(ChatColor.GREEN +"You have been charged $" + amount + "");
            }
            return true;
        } else {
            player.sendMessage(ChatColor.RED +custMessage("noMoney"));
            return false;
        }
    }

    private void saveOriginalSpawnerData(String playerName, CreatureSpawner spawner, Location location) {
        String locationString = locationToString(location);
        try {
            // Check if the location already exists in original_spawners
            if (!isLocationStored(locationString, "original_spawners")) {
                // Location is not stored, insert the data
                String insert = "INSERT INTO original_spawners (world, x, y, z, playerName, spawnerType, spawnDelay, minSpawnDelay, maxSpawnDelay, spawnCount, maxNearbyEntities, requiredPlayerRange) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error saving original spawner data: " + e.getMessage());
        }
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

    private void insertOrUpdateSpawnerData(String playerName, CreatureSpawner spawner, Location location, int level) {
        String locationString = locationToString(location);
        try {
            String insertOrUpdate = "INSERT INTO spawners (world, x, y, z, playerName, spawnerType, spawnDelay, minSpawnDelay, maxSpawnDelay, spawnCount, maxNearbyEntities, requiredPlayerRange, level) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT(world, x, y, z) DO UPDATE SET " +
                    "playerName = excluded.playerName, " +
                    "spawnerType = excluded.spawnerType, " +
                    "spawnDelay = excluded.spawnDelay, " +
                    "minSpawnDelay = excluded.minSpawnDelay, " +
                    "maxSpawnDelay = excluded.maxSpawnDelay, " +
                    "spawnCount = excluded.spawnCount, " +
                    "maxNearbyEntities = excluded.maxNearbyEntities, " +
                    "requiredPlayerRange = excluded.requiredPlayerRange, " +
                    "level = excluded.level";
            try (PreparedStatement ps = connection.prepareStatement(insertOrUpdate)) {
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
                ps.setInt(13, level);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error inserting or updating spawner data: " + e.getMessage());
        }
    }

    private void updateSpawnerFromConfig(CreatureSpawner spawner, int level) {
        String configPath = "upgrade_levels." + level;
        if (config.contains(configPath)) {
            if (spawner.getMaxSpawnDelay() < config.getInt(configPath + ".max_spawn_delay")) {
                spawner.setMaxSpawnDelay(config.getInt(configPath + ".max_spawn_delay"));
                spawner.setMinSpawnDelay(config.getInt(configPath + ".min_spawn_delay"));
            } else {
                spawner.setMinSpawnDelay(config.getInt(configPath + ".min_spawn_delay"));
                spawner.setMaxSpawnDelay(config.getInt(configPath + ".max_spawn_delay"));
            }

            spawner.setSpawnCount(config.getInt(configPath + ".spawn_count"));
            spawner.setMaxNearbyEntities(config.getInt(configPath + ".max_nearby_entities"));
            spawner.setRequiredPlayerRange(config.getInt(configPath + ".required_player_range"));
            spawner.update();
        } else {
            plugin.getLogger().warning("Level " + level + " not found in the config.");
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
