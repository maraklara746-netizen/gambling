package com.mines.plugin.commands;

import com.mines.plugin.MinesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CashoutCommand implements CommandExecutor {
    private final MinesPlugin plugin;
    public CashoutCommand(MinesPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        plugin.getGameManager().cashOut(player);
        return true;
    }
}
