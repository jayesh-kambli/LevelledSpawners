package org.first.jayesh;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.block.CreatureSpawner;

public class SpawnerUtils {

    public static void saveSpawnerData(Connection connection, Location spawnerLocation, Player player, CreatureSpawner spawner) throws SQLException {
        String locationString = spawnerLocation.getWorld().getName() + "," + spawnerLocation.getBlockX() + "," + spawnerLocation.getBlockY() + "," + spawnerLocation.getBlockZ();
        String sql = "INSERT OR REPLACE INTO spawners (location, playerName, spawnerType, spawnDelay, minSpawnDelay, maxSpawnDelay, spawnCount, maxNearbyEntities, requiredPlayerRange) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, locationString);
            stmt.setString(2, player.getName());
            stmt.setString(3, spawner.getSpawnedType().name());
            stmt.setInt(4, spawner.getDelay());
            stmt.setInt(5, spawner.getMinSpawnDelay());
            stmt.setInt(6, spawner.getMaxSpawnDelay());
            stmt.setInt(7, spawner.getSpawnCount());
            stmt.setInt(8, spawner.getMaxNearbyEntities());
            stmt.setInt(9, spawner.getRequiredPlayerRange());
            stmt.executeUpdate();
        }
    }
}
