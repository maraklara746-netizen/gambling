package com.mines.plugin.commands;

import com.mines.plugin.MinesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MinesCommand implements CommandExecutor {

    private final MinesPlugin plugin;

    public MinesCommand(MinesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            double bet = plugin.getGameManager().getBet(player);
            double balance = plugin.getDataManager().getBalance(player.getUniqueId());
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
            player.sendMessage(Component.text("💣 MINES GAME", NamedTextColor.GOLD));
            player.sendMessage(Component.text("  Balance: $" + String.format("%.2f", balance), NamedTextColor.GREEN));
            player.sendMessage(Component.text("  Current Bet: $" + String.format("%.2f", bet), NamedTextColor.YELLOW));
            player.sendMessage(Component.text("  Usage: /mines <1-18>", NamedTextColor.GRAY));
            player.sendMessage(Component.text("  Set bet: /betamount <amount>", NamedTextColor.GRAY));
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
            return true;
        }

        int mineCount;
        try {
            mineCount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("❌ Invalid number. Usage: /mines <1-18>", NamedTextColor.RED));
            return true;
        }

        plugin.getGameManager().startGame(player, mineCount);
        return true;
    }
}
