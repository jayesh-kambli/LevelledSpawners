package org.first.jayesh;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class gui implements CommandExecutor {

    private final Sigma plugin;
    private final Connection connection;
    private final FileConfiguration config;

    public gui(Sigma plugin, Connection connection, FileConfiguration config) {
        this.plugin = plugin;
        this.connection = connection;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

        if(commandSender instanceof Player) {
            Player player =(Player) commandSender;

            //target block
            Block seeSpawner = player.getTargetBlockExact(5);
//            Location spawnerLocation = new Location(Bukkit.getWorld("world"), seeSpawner.getX(), seeSpawner.getY(), seeSpawner.getZ());
            Location spawnerLocation = seeSpawner.getLocation();
            Block block = spawnerLocation.getBlock();

            Inventory inv = Bukkit.createInventory(player, 45, ChatColor.BLACK.toString() + ChatColor.BOLD + "Leveled Spawners");

            if (block.getType() == Material.SPAWNER) {
                CreatureSpawner spawner = (CreatureSpawner) block.getState();

                String confirmLv;
                List<String> updateInfo;
                int currentLevel = getSpawnerLevel(seeSpawner);
                int maxlevel = config.getInt("max_spawner_level");

                if(currentLevel == 1000) {
                    confirmLv = custMessage("Boost");
                } else {
                    confirmLv = String.valueOf(currentLevel);
                }

                if(currentLevel < maxlevel) {
                    updateInfo = Arrays.asList(ChatColor.GOLD + custMessage("level") + ": "+ ChatColor.WHITE + confirmLv+"/"+maxlevel,
                            ChatColor.GOLD+ custMessage("UpgradeCost") +  ": "+ ChatColor.WHITE +"$" + config.getInt("upgrade_levels." + (currentLevel+1) +".money"));
                } else {
                    updateInfo = Arrays.asList( ChatColor.GOLD+ custMessage("level") + ": "+ ChatColor.WHITE + confirmLv+"/"+maxlevel);
                }

                //upgrades
                ItemStack upgrade = new ItemStack(Material.ZOMBIE_HEAD);
                ItemMeta meta = upgrade.getItemMeta();
                meta.setDisplayName(ChatColor.WHITE+ "" + ChatColor.BOLD + custMessage("UpgradeSpawner"));
                meta.setLore(updateInfo);
                upgrade.setItemMeta(meta);
                inv.setItem(20, upgrade);



                //boost
                ItemStack boost;
                ItemMeta meta2;
                NamespacedKey unclickableKey = new NamespacedKey(plugin, "unClickable");
                if (isBoosterActive(seeSpawner.getWorld().getName(), seeSpawner.getX(), seeSpawner.getY(), seeSpawner.getZ())) {
                    boost = new ItemStack(Material.GREEN_BANNER);
                    meta2 = boost.getItemMeta();
                    meta2.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + custMessage("SpawnerBoost") + ChatColor.RESET + ChatColor.WHITE + " [" + ChatColor.GREEN + custMessage("Active") +ChatColor.WHITE+"]");
                    meta2.setLore(Arrays.asList(ChatColor.GREEN + "" + ChatColor.BOLD + custMessage("SpawnerIsBoosted")));
                    meta2.getPersistentDataContainer().set(unclickableKey, PersistentDataType.INTEGER, 1);;
                } else {
                    boost = new ItemStack(Material.RED_BANNER);
                    meta2 = boost.getItemMeta();
                    meta2.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + custMessage("SpawnerBoost") + ChatColor.RESET + ChatColor.WHITE + " [" + ChatColor.RED + custMessage("NotActive") +ChatColor.WHITE+"]");
                    meta2.setLore(Arrays.asList(ChatColor.GOLD + custMessage("Cost") + ": " + ChatColor.WHITE + "$" +config.getInt("upgrade_levels.1000.money"),
                            ChatColor.GOLD + custMessage("Duration") + ": " + ChatColor.WHITE + config.getInt("booster-time") + "sec"));
                }
                boost.setItemMeta(meta2);
                inv.setItem(21, boost);

                //stats
                ItemStack stats = new ItemStack(Material.BOOK);
                ItemMeta meta3 = stats.getItemMeta();
                meta3.setDisplayName(ChatColor.WHITE+ "" + ChatColor.BOLD +custMessage("Stats"));
                meta3.setLore(Arrays.asList(
                        ChatColor.GOLD + custMessage("level") + ": " + ChatColor.WHITE + confirmLv,
                        ChatColor.GOLD+ custMessage("MinDelay") +": " + ChatColor.WHITE + spawner.getMinSpawnDelay()/20 + " sec",
                        ChatColor.GOLD+ custMessage("MaxDelay") +": " + ChatColor.WHITE + spawner.getMaxSpawnDelay()/20 + " sec",
                        ChatColor.GOLD+ custMessage("SpawnCount") +": " + ChatColor.WHITE + spawner.getSpawnCount(),
                        ChatColor.GOLD+ custMessage("MaxNearby") +": " + ChatColor.WHITE + spawner.getMaxNearbyEntities(),
                        ChatColor.GOLD+ custMessage("PlayerRange") +": " + ChatColor.WHITE + spawner.getRequiredPlayerRange()));
                stats.setItemMeta(meta3);
                inv.setItem(22, stats);

                //restore
                ItemStack restore = new ItemStack(Material.EMERALD_BLOCK);
                ItemMeta meta4 = restore.getItemMeta();
                meta4.setDisplayName(ChatColor.WHITE+ "" + ChatColor.BOLD +custMessage("RestoreSpawner"));
                meta4.setLore(Arrays.asList(ChatColor.GOLD + custMessage("TurnBack")));
                restore.setItemMeta(meta4);
                inv.setItem(23, restore);

                //Close
                ItemStack Close = new ItemStack(Material.BARRIER);
                ItemMeta meta5 = Close.getItemMeta();
                meta5.setDisplayName(ChatColor.RED+ "" + ChatColor.BOLD +custMessage("close"));
                Close.setItemMeta(meta5);
                inv.setItem(24, Close);


                ItemStack pane = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                ItemMeta meta6 = restore.getItemMeta();
                meta6.setDisplayName(".");
//                meta6.setLore(Arrays.asList(""));
                for (int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44}) {
                    inv.setItem(i, pane);
                }

                player.openInventory(inv);
            }
        }
        return false;
    }

    public String custMessage(String arg) {
        MessageLoader msgLoader = new MessageLoader(config.getString("lang"));
        return msgLoader.getMessage(arg);
    }

    private boolean isBoosterActive(String world, int x, int y, int z) {
        long currentTimestamp = System.currentTimeMillis() / 1000; // Current time in seconds
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

    private int getSpawnerLevel(Block block) {
        if (block == null || block.getType() != Material.SPAWNER) {
            return 0; // Return 0 if the block is not a spawner or is null
        }

        String locationString = locationToString(block);

        try {
            String selectLevel = "SELECT level FROM spawners WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement ps = connection.prepareStatement(selectLevel)) {
                String[] parts = locationString.split(",");
                ps.setString(1, parts[0]);
                ps.setInt(2, Integer.parseInt(parts[1]));
                ps.setInt(3, Integer.parseInt(parts[2]));
                ps.setInt(4, Integer.parseInt(parts[3]));
//                plugin.getLogger().warning(parts[0] + " " + Integer.parseInt(parts[2]) + " " + Integer.parseInt(parts[1]) + " " + Integer.parseInt(parts[3]));

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
//                        plugin.getLogger().info("Found ============>");
                        return rs.getInt("level");
                    } else {
//                        plugin.getLogger().warning("Not Found ============>");
                        return 1; // Return 0 if the spawner does not exist in the table
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error retrieving spawner level: " + e.getMessage());
        }

        return 0; // Return 0 in case of any exception or error
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

}
