package com.mines.plugin.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mines.plugin.MinesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RedeemCommand implements CommandExecutor {
    private final MinesPlugin plugin;

    public RedeemCommand(MinesPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /redeem <XXXXX-XXXXX-XXXXX>", NamedTextColor.YELLOW));
            return true;
        }

        String code = args[0].toUpperCase().trim();
        player.sendMessage(Component.text("⏳ Validating code...", NamedTextColor.GRAY));

        // Run HTTP call off the main thread so the server doesn't stutter
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String botUrl = plugin.getConfig().getString("bot-api-url", "http://localhost:3000");
                String secret = plugin.getConfig().getString("http-api-secret", "changeme");

                URL url = new URL(botUrl + "/redeemcode=" + code);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-Api-Secret", secret);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int status = conn.getResponseCode();
                InputStreamReader reader = new InputStreamReader(
                    status >= 400 ? conn.getErrorStream() : conn.getInputStream()
                );
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                reader.close();

                String responseStatus = json.has("status") ? json.get("status").getAsString() : "";
                double amount = json.has("amount") ? json.get("amount").getAsDouble() : 0;

                // Jump back to main thread to modify game state
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if ("redeemed".equals(responseStatus)) {
                        plugin.getDataManager().addBalance(player.getUniqueId(), amount);
                        plugin.getScoreboardManager().update(player);
                        double newBal = plugin.getDataManager().getBalance(player.getUniqueId());
                        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
                        player.sendMessage(Component.text("🎉 Code Redeemed!", NamedTextColor.GOLD));
                        player.sendMessage(Component.text("  Added: ", NamedTextColor.GRAY).append(Component.text("+$" + String.format("%.2f", amount), NamedTextColor.GREEN)));
                        player.sendMessage(Component.text("  New Balance: ", NamedTextColor.GRAY).append(Component.text("$" + String.format("%.2f", newBal), NamedTextColor.GREEN)));
                        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
                    } else if ("already_used".equals(responseStatus)) {
                        player.sendMessage(Component.text("❌ This code has already been redeemed.", NamedTextColor.RED));
                    } else {
                        player.sendMessage(Component.text("❌ Invalid code.", NamedTextColor.RED));
                    }
                });

            } catch (Exception e) {
                plugin.getLogger().warning("[MinesPlugin] Redeem HTTP error: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(Component.text("❌ Could not reach the redemption server. Try again later.", NamedTextColor.RED))
                );
            }
        });

        return true;
    }
}
