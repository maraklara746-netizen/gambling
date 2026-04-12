package com.mines.plugin.game;

import com.mines.plugin.MinesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private final MinesPlugin plugin;
    private final Map<UUID, GameSession> activeSessions = new HashMap<>();
    private final Map<UUID, Double> betAmounts = new HashMap<>();

    public GameManager(MinesPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Bet management ────────────────────────────────────────────────────────

    public double getBet(Player player) {
        return betAmounts.getOrDefault(player.getUniqueId(), 10.0);
    }

    public boolean setBet(Player player, double amount) {
        double min = plugin.getConfig().getDouble("min-bet", 1.0);
        double max = plugin.getConfig().getDouble("max-bet", 100000.0);
        if (amount < min || amount > max) return false;
        betAmounts.put(player.getUniqueId(), amount);
        return true;
    }

    // ── Session management ────────────────────────────────────────────────────

    public boolean hasActiveSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public GameSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    /**
     * Starts a new mines game for the player.
     * Returns false if player lacks balance or already in a game.
     */
    public boolean startGame(Player player, int mineCount) {
        if (hasActiveSession(player)) {
            player.sendMessage(Component.text("❌ You already have an active game! Use /minescashout to cash out or keep playing.", NamedTextColor.RED));
            return false;
        }

        double bet = getBet(player);
        double balance = plugin.getDataManager().getBalance(player.getUniqueId());

        if (balance < bet) {
            player.sendMessage(Component.text("❌ Insufficient balance! You have $" + String.format("%.2f", balance) + " but bet is $" + String.format("%.2f", bet), NamedTextColor.RED));
            return false;
        }

        int maxMines = 18; // max mines in 6x6 grid leaving at least 18 safe tiles
        if (mineCount < 1 || mineCount > maxMines) {
            player.sendMessage(Component.text("❌ Mine count must be between 1 and " + maxMines + ".", NamedTextColor.RED));
            return false;
        }

        // Deduct bet
        plugin.getDataManager().addBalance(player.getUniqueId(), -bet);

        // Refresh scoreboard immediately after bet deduction
        plugin.getScoreboardManager().update(player);

        // Build wall
        WallFacing facing = WallFacing.fromPlayer(player);
        int distance = plugin.getConfig().getInt("wall-distance", 3);
        Location origin = facing.getWallOrigin(player, distance);

        GameSession session = new GameSession(player, bet, mineCount, origin, facing);
        activeSessions.put(player.getUniqueId(), session);

        buildWall(session);

        double newBalance = plugin.getDataManager().getBalance(player.getUniqueId());
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        player.sendMessage(Component.text("💣 MINES GAME STARTED!", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("  Bet: ", NamedTextColor.GRAY).append(Component.text("$" + String.format("%.2f", bet), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("  Mines: ", NamedTextColor.GRAY).append(Component.text(mineCount + " 💣", NamedTextColor.RED)));
        player.sendMessage(Component.text("  Balance: ", NamedTextColor.GRAY).append(Component.text("$" + String.format("%.2f", newBalance), NamedTextColor.GREEN)));
        player.sendMessage(Component.text("  Click stone blocks to reveal tiles!", NamedTextColor.AQUA));
        player.sendMessage(Component.text("  Use /minescashout to take your winnings.", NamedTextColor.AQUA));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));

        return true;
    }

    /**
     * Called when a player clicks a block on the wall.
     * Returns true if the click was handled.
     */
    public boolean handleBlockClick(Player player, Block block) {
        GameSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return false;
        if (session.getState() != GameSession.State.ACTIVE) return false;

        int index = getBlockIndex(session, block.getLocation());
        if (index == -1) return false;
        if (session.isRevealed(index)) return false;

        if (session.isMine(index)) {
            // HIT A MINE!
            session.setState(GameSession.State.LOST);
            revealMine(session, index, block);
            revealAllMines(session);
            activeSessions.remove(player.getUniqueId());
            plugin.getScoreboardManager().update(player);

            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
            player.sendMessage(Component.text("💥 BOOM! You hit a mine!", NamedTextColor.RED, TextDecoration.BOLD));
            player.sendMessage(Component.text("  You lost: ", NamedTextColor.GRAY).append(Component.text("$" + String.format("%.2f", session.getBetAmount()), NamedTextColor.RED)));
            player.sendMessage(Component.text("  Balance: ", NamedTextColor.GRAY).append(Component.text("$" + String.format("%.2f", plugin.getDataManager().getBalance(player.getUniqueId())), NamedTextColor.GREEN)));
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));

            // Schedule wall removal after 5s
            scheduleWallRemoval(session, 100L);
            return true;
        }

        // Safe tile!
        session.revealSafe(index);
        revealSafe(session, index, block);

        Map<Integer, Double> table = getMultiplierTable();
        double multiplier = session.getCurrentMultiplier(table);
        double payout = session.getCurrentPayout(table);

        player.sendMessage(Component.text("💎 Safe! ", NamedTextColor.GREEN)
                .append(Component.text("Multiplier: " + String.format("%.2fx", multiplier), NamedTextColor.YELLOW))
                .append(Component.text("  |  Cashout: ", NamedTextColor.GRAY))
                .append(Component.text("$" + String.format("%.2f", payout), NamedTextColor.GOLD)));

        // Check if all safe tiles revealed
        if (session.getSafeRevealed() == session.getTotalSafe()) {
            cashOut(player);
        }

        return true;
    }

    /**
     * Cash out current winnings.
     */
    public boolean cashOut(Player player) {
        GameSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage(Component.text("❌ You have no active game.", NamedTextColor.RED));
            return false;
        }
        if (session.getSafeRevealed() == 0) {
            player.sendMessage(Component.text("❌ You must reveal at least one tile before cashing out.", NamedTextColor.RED));
            return false;
        }

        Map<Integer, Double> table = getMultiplierTable();
        double payout = session.getCurrentPayout(table);
        double multiplier = session.getCurrentMultiplier(table);

        session.setState(GameSession.State.CASHED_OUT);
        plugin.getDataManager().addBalance(player.getUniqueId(), payout);
        activeSessions.remove(player.getUniqueId());
        plugin.getScoreboardManager().update(player);

        revealAllSafe(session);
        scheduleWallRemoval(session, 80L);

        double newBalance = plugin.getDataManager().getBalance(player.getUniqueId());
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        player.sendMessage(Component.text("💰 CASHED OUT!", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("  Multiplier: ", NamedTextColor.GRAY).append(Component.text(String.format("%.2fx", multiplier), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("  Winnings: ", NamedTextColor.GRAY).append(Component.text("+$" + String.format("%.2f", payout), NamedTextColor.GREEN)));
        player.sendMessage(Component.text("  New Balance: ", NamedTextColor.GRAY).append(Component.text("$" + String.format("%.2f", newBalance), NamedTextColor.GREEN)));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));

        return true;
    }

    public void cancelAllGames() {
        for (GameSession session : activeSessions.values()) {
            // Refund bet on shutdown
            plugin.getDataManager().addBalance(session.getPlayer().getUniqueId(), session.getBetAmount());
            removeWall(session);
        }
        activeSessions.clear();
    }

    public void cancelGame(Player player) {
        GameSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            plugin.getDataManager().addBalance(player.getUniqueId(), session.getBetAmount());
            removeWall(session);
        }
    }

    // ── Wall building ─────────────────────────────────────────────────────────

    private void buildWall(GameSession session) {
        World world = session.getPlayer().getWorld();
        WallFacing facing = session.getFacing();
        Location origin = session.getWallOrigin();

        for (int col = 0; col < 6; col++) {
            for (int row = 0; row < 6; row++) {
                Location loc = facing.getCellLocation(origin, col, row);
                loc.getBlock().setType(Material.STONE);
            }
        }
    }

    private void removeWall(GameSession session) {
        WallFacing facing = session.getFacing();
        Location origin = session.getWallOrigin();

        for (int col = 0; col < 6; col++) {
            for (int row = 0; row < 6; row++) {
                Location loc = facing.getCellLocation(origin, col, row);
                Block b = loc.getBlock();
                // Only remove blocks we placed (stone, diamond, tnt, gold)
                Material m = b.getType();
                if (m == Material.STONE || m == Material.DIAMOND_BLOCK ||
                        m == Material.TNT || m == Material.GOLD_BLOCK) {
                    b.setType(Material.AIR);
                }
            }
        }
    }

    private void scheduleWallRemoval(GameSession session, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> removeWall(session), delayTicks);
    }

    private void revealSafe(GameSession session, int index, Block block) {
        block.setType(Material.DIAMOND_BLOCK);
        session.getPlayer().getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3);
        session.getPlayer().playSound(block.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
    }

    private void revealMine(GameSession session, int index, Block block) {
        block.setType(Material.TNT);
        session.getPlayer().getWorld().spawnParticle(Particle.EXPLOSION,
                block.getLocation().add(0.5, 0.5, 0.5), 5);
        session.getPlayer().playSound(block.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
    }

    private void revealAllMines(GameSession session) {
        WallFacing facing = session.getFacing();
        Location origin = session.getWallOrigin();

        for (int index : session.getMinePositions()) {
            int col = index % 6;
            int row = index / 6;
            Location loc = facing.getCellLocation(origin, col, row);
            loc.getBlock().setType(Material.TNT);
        }
    }

    private void revealAllSafe(GameSession session) {
        WallFacing facing = session.getFacing();
        Location origin = session.getWallOrigin();

        for (int index = 0; index < 36; index++) {
            int col = index % 6;
            int row = index / 6;
            Location loc = facing.getCellLocation(origin, col, row);
            if (session.getMinePositions().contains(index)) {
                // Show mines as TNT on cashout so player can see where they were
                loc.getBlock().setType(Material.TNT);
            } else if (loc.getBlock().getType() == Material.STONE) {
                loc.getBlock().setType(Material.GOLD_BLOCK);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the cell index (0-35) for a given block location, or -1 if not part of the wall.
     */
    public int getBlockIndex(GameSession session, Location loc) {
        WallFacing facing = session.getFacing();
        Location origin = session.getWallOrigin();

        for (int col = 0; col < 6; col++) {
            for (int row = 0; row < 6; row++) {
                Location cellLoc = facing.getCellLocation(origin, col, row);
                if (cellLoc.getBlockX() == loc.getBlockX() &&
                        cellLoc.getBlockY() == loc.getBlockY() &&
                        cellLoc.getBlockZ() == loc.getBlockZ() &&
                        cellLoc.getWorld().equals(loc.getWorld())) {
                    return row * 6 + col;
                }
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Double> getMultiplierTable() {
        Map<Integer, Double> table = new HashMap<>();
        if (plugin.getConfig().isConfigurationSection("multipliers")) {
            plugin.getConfig().getConfigurationSection("multipliers").getKeys(false)
                    .forEach(k -> table.put(Integer.parseInt(k), plugin.getConfig().getDouble("multipliers." + k)));
        }
        return table;
    }
}
