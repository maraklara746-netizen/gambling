package com.mines.plugin.listeners;

import com.mines.plugin.MinesPlugin;
import com.mines.plugin.game.GameSession;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.Player;

public class BlockClickListener implements Listener {

    private final MinesPlugin plugin;

    public BlockClickListener(MinesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.STONE) return;

        GameSession session = plugin.getGameManager().getSession(player);
        if (session == null) return;

        // Check if it belongs to this player's wall
        int index = plugin.getGameManager().getBlockIndex(session, block.getLocation());
        if (index == -1) return;

        event.setCancelled(true);
        plugin.getGameManager().handleBlockClick(player, block);
    }
}
