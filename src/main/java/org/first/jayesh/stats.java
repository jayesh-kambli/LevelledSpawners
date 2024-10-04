package org.first.jayesh;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class stats implements CommandExecutor {

    private final JavaPlugin plugin;
    private BukkitTask task;
    private boolean taskRunning;

    public stats(JavaPlugin plugin) {
        this.plugin = plugin;
        this.taskRunning = false;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(commandSender instanceof Player) {
            Player player = (Player) commandSender;
//            if (player.hasPermission("sigma.open")) {
                if (command.getName().equalsIgnoreCase("cstat")) {
                    if (taskRunning) {
                        cancelTask();
                        player.sendMessage("Task stopped.");
                    } else {
                        Block seeSpawner = player.getTargetBlockExact(5);
                        Location spawnerLocation = new Location(Bukkit.getWorld("world"), seeSpawner.getX(), seeSpawner.getY(), seeSpawner.getZ());
                        Block block = spawnerLocation.getBlock();
                        if (block.getType() == Material.SPAWNER) {
                            task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                                if (block.getType() == Material.SPAWNER) {
                                    CreatureSpawner spawner = (CreatureSpawner) block.getState();
                                    EntityType spawnType = spawner.getSpawnedType();
                                    int spawnDelay = spawner.getDelay();
                                    int minSpawnDelay = spawner.getMinSpawnDelay();
                                    int maxSpawnDelay = spawner.getMaxSpawnDelay();
                                    int spawnCount = spawner.getSpawnCount();
                                    int maxNearbyEntities = spawner.getMaxNearbyEntities();
                                    int requiredPlayerRange = spawner.getRequiredPlayerRange();

                                    // Send spawner information to the
                                    player.sendMessage("========================");
                                    player.sendMessage("Spawner Type: " + spawnType);
                                    player.sendMessage("Spawn Delay: " + spawnDelay + " ticks");
                                    player.sendMessage("Min Spawn Delay: " + minSpawnDelay + " ticks");
                                    player.sendMessage("Max Spawn Delay: " + maxSpawnDelay + " ticks");
                                    player.sendMessage("Spawn Count: " + spawnCount);
                                    player.sendMessage("Max Nearby Entities: " + maxNearbyEntities);
                                    player.sendMessage("Required Player Range: " + requiredPlayerRange);
                                    player.sendMessage("========================");
                                    player.sendMessage(" ");

                                } else {
                                    player.sendMessage("No Spawner Detected !!");
                                }
                            }, 0L, 40L); // Run every 2 seconds (40 ticks)
                            taskRunning = true; // Mark task as running
                        }
                    }
                    player.sendMessage("No task running to stop.");
                }
                return true;
//            } else {
//                player.sendMessage("You don't have permission to use this command.");
//            }
        }

            return false;
    }
    public void cancelTask() {
        if (task != null) {
            task.cancel();
            taskRunning = false; // Mark task as not running
        }
    }
}
