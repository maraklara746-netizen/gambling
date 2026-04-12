package com.mines.plugin.commands;

import com.mines.plugin.MinesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    private final MinesPlugin plugin;
    public AdminCommand(MinesPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mines.admin")) {
            sender.sendMessage(Component.text("❌ No permission.", NamedTextColor.RED)); return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /minesadmin <give|set|reset> <player> [amount]", NamedTextColor.YELLOW)); return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(Component.text("❌ Player not found.", NamedTextColor.RED)); return true; }

        switch (args[0].toLowerCase()) {
            case "give" -> {
                if (args.length < 3) { sender.sendMessage("Usage: /minesadmin give <player> <amount>"); return true; }
                double amt; try { amt = Double.parseDouble(args[2]); } catch (NumberFormatException e) { sender.sendMessage("Invalid amount."); return true; }
                plugin.getDataManager().addBalance(target.getUniqueId(), amt);
                plugin.getScoreboardManager().update(target);
                sender.sendMessage(Component.text("✅ Gave $" + String.format("%.2f", amt) + " to " + target.getName(), NamedTextColor.GREEN));
                target.sendMessage(Component.text("💰 An admin gave you $" + String.format("%.2f", amt) + "!", NamedTextColor.GREEN));
            }
            case "set" -> {
                if (args.length < 3) { sender.sendMessage("Usage: /minesadmin set <player> <amount>"); return true; }
                double amt; try { amt = Double.parseDouble(args[2]); } catch (NumberFormatException e) { sender.sendMessage("Invalid amount."); return true; }
                plugin.getDataManager().setBalance(target.getUniqueId(), amt);
                plugin.getScoreboardManager().update(target);
                sender.sendMessage(Component.text("✅ Set " + target.getName() + "'s balance to $" + String.format("%.2f", amt), NamedTextColor.GREEN));
            }
            case "reset" -> {
                double startBal = plugin.getConfig().getDouble("starting-balance", 100.0);
                plugin.getDataManager().setBalance(target.getUniqueId(), startBal);
                plugin.getScoreboardManager().update(target);
                sender.sendMessage(Component.text("✅ Reset " + target.getName() + "'s balance to $" + startBal, NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Unknown subcommand. Use give/set/reset.", NamedTextColor.RED));
        }
        return true;
    }
}
