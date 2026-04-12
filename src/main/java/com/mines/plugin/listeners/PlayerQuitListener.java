package com.mines.plugin.listeners;

import com.mines.plugin.MinesPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final MinesPlugin plugin;

    public PlayerQuitListener(MinesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getGameManager().hasActiveSession(event.getPlayer())) {
            plugin.getGameManager().cancelGame(event.getPlayer());
        }
    }
}
