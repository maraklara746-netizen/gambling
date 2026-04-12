package com.mines.plugin.commands;

import com.mines.plugin.MinesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RedeemCommand implements CommandExecutor {
    private final MinesPlugin plugin;
    public RedeemCommand(MinesPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /redeem <XXXXX-XXXXX-XXXXX>", NamedTextColor.YELLOW)); return true;
        }
        String code = args[0].toUpperCase().trim();
        double result = plugin.getKeysManager().redeem(code);
        if (result == -1) {
            player.sendMessage(Component.text("❌ Invalid code.", NamedTextColor.RED)); return true;
        }
        if (result == -2) {
            player.sendMessage(Component.text("❌ This code has already been redeemed.", NamedTextColor.RED)); return true;
        }
        plugin.getDataManager().addBalance(player.getUniqueId(), result);
        double newBal = plugin.getDataManager().getBalance(player.getUniqueId());
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        player.sendMessage(Component.text("🎉 Code Redeemed!", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  Added: ", NamedTextColor.GRAY).append(Component.text("+$" + String.format("%.2f", result), NamedTextColor.GREEN)));
        player.sendMessage(Component.text("  New Balance: ", NamedTextColor.GRAY).append(Component.text("$" + String.format("%.2f", newBal), NamedTextColor.GREEN)));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        return true;
    }
}
