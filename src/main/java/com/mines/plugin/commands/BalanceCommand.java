package com.mines.plugin.commands;

import com.mines.plugin.MinesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {
    private final MinesPlugin plugin;
    public BalanceCommand(MinesPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        double balance = plugin.getDataManager().getBalance(player.getUniqueId());
        double bet = plugin.getGameManager().getBet(player);
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        player.sendMessage(Component.text("💰 Your Mines Balance", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  Balance: ", NamedTextColor.GRAY).append(Component.text("$" + String.format("%.2f", balance), NamedTextColor.GREEN)));
        player.sendMessage(Component.text("  Current Bet: ", NamedTextColor.GRAY).append(Component.text("$" + String.format("%.2f", bet), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        return true;
    }
}
