package com.mines.plugin.listeners;

import com.mines.plugin.MinesPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final MinesPlugin plugin;

    public PlayerJoinListener(MinesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Show scoreboard as soon as the player connects
        plugin.getScoreboardManager().update(event.getPlayer());
    }
}
