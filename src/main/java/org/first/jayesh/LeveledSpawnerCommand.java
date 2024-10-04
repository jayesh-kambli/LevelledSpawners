package org.first.jayesh;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeveledSpawnerCommand implements CommandExecutor {

    private final Sigma plugin;
    private final RestoreAllSpawnerData restoreAllSpawnerData;
    private final SeeAllSpawnerData seeAllSpawnerData;

    public LeveledSpawnerCommand(Sigma plugin, RestoreAllSpawnerData restoreAllSpawnerData, SeeAllSpawnerData seeAllSpawnerData) {
        this.plugin = plugin;
        this.restoreAllSpawnerData = restoreAllSpawnerData;
        this.seeAllSpawnerData = seeAllSpawnerData;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("Usage: /leveledSpawner <RestoreAllSpawnerData|SeeAllSpawnerData>");
            return true;
        }

        if (args[0].equalsIgnoreCase("RestoreAllSpawnerData")) {
            return restoreAllSpawnerData.onCommand(sender, command, label, args);
        } else if (args[0].equalsIgnoreCase("SeeAllSpawnerData")) {
            return seeAllSpawnerData.onCommand(sender, command, label, args);
        } else {
            player.sendMessage("Unknown subcommand. Usage: /leveledSpawner <RestoreAllSpawnerData|SeeAllSpawnerData>");
        }

        return true;
    }
}
