package org.first.jayesh;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class menuListner implements Listener {

    private final Sigma plugin;
    private final Connection connection;

    public menuListner(Sigma plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }


    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = ChatColor.translateAlternateColorCodes('&', e.getView().getTitle());
        if(title.equals(org.bukkit.ChatColor.BLACK.toString() + org.bukkit.ChatColor.BOLD + "Leveled Spawners") && e.getCurrentItem() != null) {
            e.setCancelled(true);
            Player player = (Player) e.getWhoClicked();
            switch (e.getRawSlot()) {
                case 20:
                    player.closeInventory();
//                    player.sendMessage("Triggering /ccheck command...");
//                    if (plugin.getCheckBlockCommand() != null) {
//                        plugin.getCheckBlockCommand().onCommand(player, null, "ccheck", new String[]{"40", "10"}); // Modify arguments accordingly
//                    }
                    upgradeSpawner(player);
                    break;
                case 21:
                    //boost
                    boostSpawner(player);
                    break;
                case 23:
                    player.closeInventory();
//                    player.sendMessage("Triggering /crestore command...");
                    if (plugin.getRestoreBlockCommand() != null) {
                        plugin.getRestoreBlockCommand().onCommand(player, null, "crestore", new String[]{});
                    }
                    break;
                case 24:
                    player.sendMessage("Closed");
                    break;
                default:
                    return;

            }


            player.closeInventory();
        }
    }

    public String custMessage(String arg) {
        @NotNull FileConfiguration config = plugin.getConfig();
        MessageLoader msgLoader = new MessageLoader(config.getString("lang"));
        return msgLoader.getMessage(arg);
    }

    private void upgradeSpawner(Player player) {
        Block clickedBlock = player.getTargetBlockExact(10);

        if (clickedBlock != null && clickedBlock.getType() == Material.SPAWNER) {
            String locationString = locationToString(clickedBlock);
            World world = clickedBlock.getWorld();
            int x = clickedBlock.getX();
            int y = clickedBlock.getY();
            int z = clickedBlock.getZ();

            // Convert the World object to a String (world name)
            String worldName = world.getName();

            //Check if booster is active
//            player.sendMessage("IsBooster => "+isBoosterActive(worldName, x, y, z));
            if(!isBoosterActive(worldName, x, y, z)) {
                try {
                    String selectOriginal = "SELECT level FROM spawners WHERE world = ? AND x = ? AND y = ? AND z = ?";
                    try (PreparedStatement ps = connection.prepareStatement(selectOriginal)) {
                        String[] parts = locationString.split(",");
                        ps.setString(1, parts[0]);
                        ps.setInt(2, Integer.parseInt(parts[1]));
                        ps.setInt(3, Integer.parseInt(parts[2]));
                        ps.setInt(4, Integer.parseInt(parts[3]));

                        int currentLevel = 1;

                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                currentLevel = rs.getInt("level");
//                                player.sendMessage("Previoius Update Found ==> " + locationString);
                            } else {
//                                player.sendMessage("No Original Data Found ==> " + locationString);
                            }
                        }


                        int maxLevel = plugin.getConfig().getInt("max_spawner_level");

                        if (currentLevel < maxLevel) {
                            int newLevel = currentLevel + 1;
                            updateSpawnerLevel(locationString, newLevel);
                            plugin.getCheckBlockCommand().onCommand(player, null, "ccheck", new String[]{String.valueOf(newLevel)});


//                            player.sendMessage(ChatColor.GREEN + "Spawner upgraded to level " + newLevel);
                        } else if(currentLevel == 1000) {
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + custMessage("TryAfterSpawnerBoostingIsExpired"));
                        } else {
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + custMessage("Spawneralreadyatmaximumlevel") + "(" + currentLevel+")");
                        }
                    }
                } catch (SQLException e) {
                    plugin.getLogger().warning("Error restoring spawner data: " + e.getMessage());
                }
            } else {
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + custMessage("UpdateAfterBoostIsExpired"));
            }
        } else {
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + custMessage("noSpawnerDetected"));
            return;
        }
    }

    private void updateSpawnerLevel(String locationString, int newLevel) {
        try {
            String updateLevel = "UPDATE spawners SET level = ? WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateLevel)) {
                String[] parts = locationString.split(",");
                ps.setInt(1, newLevel);
                ps.setString(2, parts[0]);
                ps.setInt(3, Integer.parseInt(parts[1]));
                ps.setInt(4, Integer.parseInt(parts[2]));
                ps.setInt(5, Integer.parseInt(parts[3]));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error updating spawner level: " + e.getMessage());
        }
    }


    private void boostSpawner(Player player) {
        // Logic to determine which spawner to boost (retrieve coordinates, etc.)
        String worldName = player.getWorld().getName();
        int x = 0; // Example coordinates, replace with actual logic to get spawner coordinates
        int y = 0;
        int z = 0;

        // Replace with actual logic to determine x, y, z coordinates of the spawner
        Block clickedBlock = player.getTargetBlockExact(10); // Example: get the targeted block within 10 blocks range
        if (clickedBlock != null && clickedBlock.getType() == Material.SPAWNER) {
            x = clickedBlock.getX();
            y = clickedBlock.getY();
            z = clickedBlock.getZ();
        } else {
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + custMessage("noSpawnerDetected"));
            return;
        }

        long boostDurationSeconds = plugin.getConfig().getInt("booster-time");

        // Add booster to database and execute /ccheck command
        plugin.getBoosterManager().addBooster(worldName, x, y, z, boostDurationSeconds, player.getName());
    }

    private String locationToString(Block block) {
        if (block == null) {
            return null;
        }
        return String.format("%s,%d,%d,%d",
                block.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ());
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
