package org.first.jayesh;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SeeAllSpawnerData implements CommandExecutor, Listener {

    private final JavaPlugin plugin; // Field to store the plugin instance
    private final Connection connection; // Database connection
    private final NamespacedKey spawnLocationKey; // Key for storing location in item metadata

    public SeeAllSpawnerData(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
        this.spawnLocationKey = new NamespacedKey(plugin, "spawn_location");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            openSpawnerDataGUI(player);
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

    private void openSpawnerDataGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 54, custMessage("SpawnersData"));

        // Fetch and display data from the database
        try {
            String selectQuery = "SELECT * FROM spawners INNER JOIN original_spawners " +
                    "ON spawners.world = original_spawners.world " +
                    "AND spawners.x = original_spawners.x " +
                    "AND spawners.y = original_spawners.y " +
                    "AND spawners.z = original_spawners.z";

            try (PreparedStatement ps = connection.prepareStatement(selectQuery)) {
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
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

                    // Format data for GUI display
                    String title = String.format("Spawner at %s, %d, %d, %d", world, x, y, z);
                    ItemStack item = createGUIItem(Material.SPAWNER, title,
                             ChatColor.GRAY+ "[ Click To Teleport ]",
                            ChatColor.GOLD + custMessage("PlayerName") +": " + ChatColor.WHITE + playerName,
                            ChatColor.GOLD + custMessage("SpawnerType") + ": " + ChatColor.WHITE + spawnerType,
                            ChatColor.GOLD + custMessage("SpawnDelay") + ": " + ChatColor.WHITE + spawnDelay,
                            ChatColor.GOLD + custMessage("MinSpawnDelay") + ": " + ChatColor.WHITE + minSpawnDelay,
                            ChatColor.GOLD + custMessage("MaxSpawnDelay") +": " + ChatColor.WHITE + maxSpawnDelay,
                            ChatColor.GOLD + custMessage("SpawnCount") + ": " + ChatColor.WHITE + spawnCount,
                            ChatColor.GOLD + custMessage("MaxNearby") +": " + ChatColor.WHITE + maxNearbyEntities,
                            ChatColor.GOLD + custMessage("RequiredPlayerRange") +": " + ChatColor.WHITE + requiredPlayerRange);

                    // Store location details in the item's metadata
                    ItemMeta meta = item.getItemMeta();
                    meta.getPersistentDataContainer().set(spawnLocationKey, PersistentDataType.STRING, String.format("%s;%d;%d;%d", world, x, y, z));
                    item.setItemMeta(meta);

                    gui.addItem(item);
                }

                // Register the InventoryClickEvent listener
                Bukkit.getPluginManager().registerEvents(new InventoryClickListener(), plugin);

                player.openInventory(gui);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error fetching spawner data: " + e.getMessage());
        }
    }

    private ItemStack createGUIItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(line);
        }
        meta.setLore(loreList);

        item.setItemMeta(meta);
        return item;
    }

    private class InventoryClickListener implements Listener {

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            String title = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', event.getView().getTitle());
            if(title.equals(custMessage("SpawnersData")) && event.getCurrentItem() != null) {
                if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof Player) {
                    Player player = (Player) event.getClickedInventory().getHolder();

                    if (event.getInventory().equals(player.getOpenInventory().getTopInventory())) {
                        event.setCancelled(true); // Cancel the event to prevent taking items from GUI

                        ItemStack clickedItem = event.getCurrentItem();
                        if (clickedItem != null && clickedItem.getType() == Material.SPAWNER) {
                            // Retrieve location details from item metadata
                            ItemMeta meta = clickedItem.getItemMeta();
                            if (meta != null && meta.getPersistentDataContainer().has(spawnLocationKey, PersistentDataType.STRING)) {
                                String locationString = meta.getPersistentDataContainer().get(spawnLocationKey, PersistentDataType.STRING);
                                String[] parts = locationString.split(";");
                                if (parts.length == 4) {
                                    String worldName = parts[0];
                                    int x = Integer.parseInt(parts[1]);
                                    int y = Integer.parseInt(parts[2]);
                                    int z = Integer.parseInt(parts[3]);

                                    // Teleport the player to the spawner location
                                    player.teleport(new org.bukkit.Location(Bukkit.getWorld(worldName), x + 0.5, y, z + 0.5));
                                    player.sendMessage("Teleported to spawner at " + worldName + ", " + x + ", " + y + ", " + z);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
