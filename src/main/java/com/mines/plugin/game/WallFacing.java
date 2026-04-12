package com.mines.plugin.game;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public enum WallFacing {
    NORTH, SOUTH, EAST, WEST;

    /**
     * Returns the wall facing direction based on player's yaw.
     * The wall faces the player, so it's built in front of them.
     */
    public static WallFacing fromPlayer(Player player) {
        float yaw = player.getLocation().getYaw();
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw >= 315 || yaw < 45) return SOUTH;
        if (yaw >= 45 && yaw < 135) return WEST;
        if (yaw >= 135 && yaw < 225) return NORTH;
        return EAST;
    }

    /**
     * Gets the location of a specific cell (col, row) in the 6x6 grid
     * given the wall origin (bottom-left corner facing the player).
     */
    public Location getCellLocation(Location origin, int col, int row) {
        Location loc = origin.clone();
        switch (this) {
            case SOUTH:
                loc.add(col, row, 0);
                break;
            case NORTH:
                loc.add(-col, row, 0);
                break;
            case EAST:
                loc.add(0, row, col);
                break;
            case WEST:
                loc.add(0, row, -col);
                break;
        }
        return loc;
    }

    /**
     * Returns the origin (bottom-left) for the wall, placed in front of the player.
     */
    public Location getWallOrigin(Player player, int distance) {
        Location base = player.getLocation().getBlock().getLocation();
        switch (this) {
            case SOUTH:
                base.add(-2, 0, distance);  // offset left by half width (3) so wall is centered
                break;
            case NORTH:
                base.add(3, 0, -distance);
                break;
            case EAST:
                base.add(distance, 0, -2);
                break;
            case WEST:
                base.add(-distance, 0, 3);
                break;
        }
        return base;
    }
}
