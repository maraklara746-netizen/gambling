package com.mines.plugin.scoreboard;

import com.mines.plugin.MinesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final MinesPlugin plugin;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    public ScoreboardManager(MinesPlugin plugin) {
        this.plugin = plugin;
    }

    /** Show (or refresh) the sidebar scoreboard for a player. */
    public void update(Player player) {
        Scoreboard board = scoreboards.computeIfAbsent(player.getUniqueId(), k -> {
            Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = sb.registerNewObjective(
                "blockgamba", Criteria.DUMMY,
                Component.text("  \uD83C\uDFB0 BlockGamba  ", NamedTextColor.GOLD, TextDecoration.BOLD)
            );
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            return sb;
        });

        Objective obj = board.getObjective("blockgamba");
        if (obj == null) return;

        double balance = plugin.getDataManager().getBalance(player.getUniqueId());
        double bet     = plugin.getGameManager().getBet(player);

        // Clear old entries
        for (String entry : board.getEntries()) board.resetScores(entry);

        // Scores render highest → lowest (top to bottom)
        fakeScore(obj, " ",                                                                        7);
        fakeScore(obj, "§e\uD83D\uDCB0 Balance§7: §a$" + String.format("%.2f", balance),          6);
        fakeScore(obj, "  ",                                                                       5);
        fakeScore(obj, "§e\uD83C\uDFB2 Current Bet§7: §b$" + String.format("%.2f", bet),          4);
        fakeScore(obj, "   ",                                                                      3);
        fakeScore(obj, "    ",                                                                     2);
        fakeScore(obj, "§7\uD83C\uDF10 blockgamba.net",                                            1);

        player.setScoreboard(board);
    }

    /** Restore the server's main scoreboard for a player. */
    public void remove(Player player) {
        scoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /** Clean up on plugin disable. */
    public void removeAll() {
        for (UUID uuid : scoreboards.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        scoreboards.clear();
    }

    private void fakeScore(Objective obj, String text, int score) {
        obj.getScore(text).setScore(score);
    }
}
