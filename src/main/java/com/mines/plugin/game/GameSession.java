package com.mines.plugin.game;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class GameSession {

    public enum State { ACTIVE, WON, LOST, CASHED_OUT }

    private final Player player;
    private final double betAmount;
    private final int mineCount;
    private final int totalCells; // 6x6 = 36
    private final Set<Integer> minePositions; // indices 0-35
    private final Set<Integer> revealedSafe;
    private State state;

    // Wall anchor: bottom-left block of the 6x6 grid
    private Location wallOrigin;

    // The facing direction used to build the wall
    private WallFacing facing;

    public GameSession(Player player, double betAmount, int mineCount, Location wallOrigin, WallFacing facing) {
        this.player = player;
        this.betAmount = betAmount;
        this.mineCount = mineCount;
        this.totalCells = 36;
        this.wallOrigin = wallOrigin;
        this.facing = facing;
        this.revealedSafe = new HashSet<>();
        this.state = State.ACTIVE;

        // Randomly place mines
        this.minePositions = new HashSet<>();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < totalCells; i++) indices.add(i);
        Collections.shuffle(indices);
        for (int i = 0; i < mineCount; i++) minePositions.add(indices.get(i));
    }

    public boolean isMine(int index) { return minePositions.contains(index); }

    public void revealSafe(int index) { revealedSafe.add(index); }

    public int getSafeRevealed() { return revealedSafe.size(); }

    public boolean isRevealed(int index) { return revealedSafe.contains(index) || minePositions.contains(index); }

    /** Returns current multiplier based on safe tiles revealed */
    public double getCurrentMultiplier(Map<Integer, Double> multiplierTable) {
        int revealed = revealedSafe.size();
        if (revealed == 0) return 1.0;
        double base = multiplierTable.getOrDefault(mineCount, 1.1);
        return Math.pow(base, revealed);
    }

    public double getCurrentPayout(Map<Integer, Double> multiplierTable) {
        return betAmount * getCurrentMultiplier(multiplierTable);
    }

    public Player getPlayer() { return player; }
    public double getBetAmount() { return betAmount; }
    public int getMineCount() { return mineCount; }
    public Set<Integer> getMinePositions() { return minePositions; }
    public Set<Integer> getRevealedSafe() { return revealedSafe; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public Location getWallOrigin() { return wallOrigin; }
    public WallFacing getFacing() { return facing; }
    public int getTotalCells() { return totalCells; }
    public int getTotalSafe() { return totalCells - mineCount; }
}
