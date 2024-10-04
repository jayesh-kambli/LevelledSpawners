package org.first.jayesh;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public final class Sigma extends JavaPlugin implements Listener {

    private Connection connection;
    private checkBlock checkBlockCommand;
    private restore restoreBlockCommand;
    private BoosterManager boosterManager;
    private FileConfiguration config;
    private static Economy economy = null;

    @Override
    public void onEnable(){
        if (!setupEconomy()) {
            getLogger().severe("Vault is required for this plugin to work. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveResourceIfNotExists("lang/messages_en.properties");
        saveResourceIfNotExists("lang/messages_fr.properties");

        connectToDatabase();

        // Save default config if not present
        saveDefaultConfig();

        // Load config
        config = getConfig();
        MessageLoader englishLoader = new MessageLoader(config.getString("lang"));
        String welcomeMessage = englishLoader.getMessage("welcome");
        System.out.println(welcomeMessage);


        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new menuListner(this, getDatabaseConnection()), this); // Register menuListner


        checkBlockCommand = new checkBlock(this, getDatabaseConnection(), config, economy); // Added line
        restoreBlockCommand = new restore(this, getDatabaseConnection()); // Added line
        getCommand("ccheck").setExecutor(checkBlockCommand);
        getCommand("cstat").setExecutor(new stats(this));
        getCommand("crestore").setExecutor(new restore(this, getDatabaseConnection()));
        getCommand("ccgui").setExecutor(new gui(this, getDatabaseConnection(), config));

//        getCommand("RestoreAllSpawnerData").setExecutor(new RestoreAllSpawnerData(this, getDatabaseConnection()));
//        getCommand("SeeAllSpawnerData").setExecutor(new SeeAllSpawnerData (this, getDatabaseConnection()));
//        Bukkit.getPluginManager().registerEvents(new menuListner(this), this);

        RestoreAllSpawnerData restoreAllSpawnerData = new RestoreAllSpawnerData(this, getDatabaseConnection());
        SeeAllSpawnerData seeAllSpawnerData = new SeeAllSpawnerData(this, getDatabaseConnection());

        getCommand("leveledSpawner").setExecutor(new LeveledSpawnerCommand(this, restoreAllSpawnerData, seeAllSpawnerData));
        getCommand("leveledSpawner").setTabCompleter(new LeveledSpawnerTabCompleter());

        boosterManager = new BoosterManager(this, getDatabaseConnection(), config, economy);
        boosterManager.startScheduler();
    }


    private void connectToDatabase() {
        try {
            // Create the plugins directory if it doesn't exist
            File pluginDir = new File(getDataFolder().getPath());
            if (!pluginDir.exists()) {
                pluginDir.mkdirs();
            }

            // Connect to the SQLite database
            String databasePath = "jdbc:sqlite:" + getDataFolder() + "/database.db";
            connection = DriverManager.getConnection(databasePath);
            getLogger().info("Connected to SQLite database.");
            createSpawnerDataTable();
            createOriginalSpawnerDataTable();
            createBoosterDataTable();
            createBoosterBackupTable();
        } catch (SQLException e) {
            getLogger().warning("Failed to connect to SQLite database: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (connection != null) {
            try {
                connection.close();
                getLogger().info("Disconnected from SQLite database.");
            } catch (SQLException e) {
                getLogger().warning("Error closing SQLite connection: " + e.getMessage());
            }
        }
    }
    public Connection getDatabaseConnection() {
        return connection;
    }

    public checkBlock getCheckBlockCommand() {
        return checkBlockCommand;
    }

    public restore getRestoreBlockCommand() {
        return restoreBlockCommand;
    }
    public BoosterManager getBoosterManager() {
        return boosterManager;
    }

    private void createSpawnerDataTable() {
        try (Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS spawners (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "level INTEGER," +
                    "world TEXT," +  // Add world column
                    "x INTEGER," +   // Add x column
                    "y INTEGER," +   // Add y column
                    "z INTEGER," +   // Add z column
                    "playerName TEXT," +
                    "spawnerType TEXT," +
                    "spawnDelay INTEGER," +
                    "minSpawnDelay INTEGER," +
                    "maxSpawnDelay INTEGER," +
                    "spawnCount INTEGER," +
                    "maxNearbyEntities INTEGER," +
                    "requiredPlayerRange INTEGER," +
                    "UNIQUE(world, x, y, z)" + // Define a unique constraint on world, x, y, z
                    ")";
            statement.execute(sql);
            getLogger().info("Created spawners table if not exists.");
        } catch (SQLException e) {
            getLogger().warning("Error creating spawners table: " + e.getMessage());
        }
    }

    private void createOriginalSpawnerDataTable() {
        try (Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS original_spawners (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "world TEXT," +
                    "x INTEGER," +
                    "y INTEGER," +
                    "z INTEGER," +
                    "playerName TEXT," +
                    "spawnerType TEXT," +
                    "spawnDelay INTEGER," +
                    "minSpawnDelay INTEGER," +
                    "maxSpawnDelay INTEGER," +
                    "spawnCount INTEGER," +
                    "maxNearbyEntities INTEGER," +
                    "requiredPlayerRange INTEGER," +
                    "UNIQUE(world, x, y, z)" +
                    ")";
            statement.execute(sql);
            getLogger().info("Created original_spawners table if not exists.");
        } catch (SQLException e) {
            getLogger().warning("Error creating original_spawners table: " + e.getMessage());
        }
    }


    private void createBoosterDataTable() {
        try (Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS boosters (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "world TEXT," +  // Add world column
                    "playerName TEXT," +
                    "x INTEGER," +   // Add x column
                    "y INTEGER," +   // Add y column
                    "z INTEGER," +   // Add z column
                    "startTime INTEGER," + // Timestamp when booster started
                    "endTime INTEGER" +    // Timestamp when booster should end
                    ")";
            statement.execute(sql);
            getLogger().info("Created boosters table if not exists.");
        } catch (SQLException e) {
            getLogger().warning("Error creating boosters table: " + e.getMessage());
        }
    }

    private void createBoosterBackupTable() {
        try (Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS booster_backup (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "level INTEGER," +
                    "world TEXT," +  // Add world column
                    "x INTEGER," +   // Add x column
                    "y INTEGER," +   // Add y column
                    "z INTEGER," +   // Add z column
                    "playerName TEXT," +
                    "spawnerType TEXT," +
                    "spawnDelay INTEGER," +
                    "minSpawnDelay INTEGER," +
                    "maxSpawnDelay INTEGER," +
                    "spawnCount INTEGER," +
                    "maxNearbyEntities INTEGER," +
                    "requiredPlayerRange INTEGER," +
                    "UNIQUE(world, x, y, z)" + // Define a unique constraint on world, x, y, z
                    ")";
            statement.execute(sql);
            getLogger().info("Created Booster backup table if not exists.");
        } catch (SQLException e) {
            getLogger().warning("Error creating Booster Backup table: " + e.getMessage());
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        return true;
//        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
//        if (rsp == null) {
//            return false;
//        }
//        economy = rsp.getProvider();
//        return economy != null;
    }

    public static Economy getEconomy() {
        return economy;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getPlayer().isSneaking() && clickedBlock != null && clickedBlock.getType() == Material.SPAWNER && player.getInventory().getItemInHand().getType() == Material.IRON_INGOT) {
//                player.sendMessage("Spotted!");
                getServer().dispatchCommand(event.getPlayer(), "ccgui");
        }
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File outFile = new File(getDataFolder(), resourcePath);
        if (!outFile.exists()) {
            outFile.getParentFile().mkdirs(); // Create directories if they don't exist
            try (InputStream in = getResource(resourcePath)) {
                if (in != null) {
                    Files.copy(in, outFile.toPath());
                } else {
                    getLogger().warning("Resource not found: " + resourcePath);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
