package com.mines.plugin.commands;

import com.mines.plugin.MinesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.mines.plugin.scoreboard.ScoreboardManager;

public class BetAmountCommand implements CommandExecutor {
    private final MinesPlugin plugin;
    public BetAmountCommand(MinesPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (args.length == 0) {
            double current = plugin.getGameManager().getBet(player);
            player.sendMessage(Component.text("Current bet: $" + String.format("%.2f", current) + " | Usage: /betamount <amount>", NamedTextColor.YELLOW));
            return true;
        }
        double amount;
        try { amount = Double.parseDouble(args[0]); } catch (NumberFormatException e) {
            player.sendMessage(Component.text("❌ Invalid amount.", NamedTextColor.RED)); return true;
        }
        if (plugin.getGameManager().hasActiveSession(player)) {
            player.sendMessage(Component.text("❌ Cannot change bet during an active game.", NamedTextColor.RED)); return true;
        }
        if (!plugin.getGameManager().setBet(player, amount)) {
            double min = plugin.getConfig().getDouble("min-bet", 1.0);
            double max = plugin.getConfig().getDouble("max-bet", 100000.0);
            player.sendMessage(Component.text("❌ Bet must be between $" + min + " and $" + max, NamedTextColor.RED)); return true;
        }
        plugin.getScoreboardManager().update(player);
        player.sendMessage(Component.text("✅ Bet set to $" + String.format("%.2f", amount), NamedTextColor.GREEN));
        return true;
    }
}
