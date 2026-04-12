package com.mines.plugin;

import com.mines.plugin.commands.*;
import com.mines.plugin.game.GameManager;
import com.mines.plugin.data.DataManager;
import com.mines.plugin.data.KeysManager;
import com.mines.plugin.http.KeysHttpServer;
import com.mines.plugin.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MinesPlugin extends JavaPlugin {

    private static MinesPlugin instance;
    private GameManager gameManager;
    private DataManager dataManager;
    private KeysManager keysManager;
    private KeysHttpServer httpServer;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.dataManager       = new DataManager(this);
        this.keysManager       = new KeysManager(this);
        this.gameManager       = new GameManager(this);
        this.scoreboardManager = new ScoreboardManager(this);

        // Start HTTP API so the Discord bot can create/check keys remotely
        this.httpServer = new KeysHttpServer(this);
        try {
            httpServer.start();
        } catch (Exception e) {
            getLogger().severe("[MinesPlugin] Failed to start HTTP API: " + e.getMessage());
        }

        // Register commands
        getCommand("mines").setExecutor(new MinesCommand(this));
        getCommand("betamount").setExecutor(new BetAmountCommand(this));
        getCommand("redeem").setExecutor(new RedeemCommand(this));
        getCommand("minesbalance").setExecutor(new BalanceCommand(this));
        getCommand("minescashout").setExecutor(new CashoutCommand(this));
        getCommand("minesadmin").setExecutor(new AdminCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new com.mines.plugin.listeners.BlockClickListener(this), this);
        getServer().getPluginManager().registerEvents(new com.mines.plugin.listeners.PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new com.mines.plugin.listeners.PlayerJoinListener(this), this);

        // Show scoreboard to any players already online (e.g. after /reload)
        for (Player p : Bukkit.getOnlinePlayers()) scoreboardManager.update(p);

        // Refresh scoreboards every 4 seconds so balance stays live
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) scoreboardManager.update(p);
        }, 80L, 80L);

        getLogger().info("MinesPlugin enabled!");
    }

    @Override
    public void onDisable() {
        if (httpServer != null) httpServer.stop();
        if (gameManager != null) gameManager.cancelAllGames();
        if (dataManager != null) dataManager.saveAll();
        if (scoreboardManager != null) scoreboardManager.removeAll();
        getLogger().info("MinesPlugin disabled.");
    }

    public static MinesPlugin getInstance() { return instance; }
    public GameManager getGameManager()         { return gameManager; }
    public DataManager getDataManager()         { return dataManager; }
    public KeysManager getKeysManager()         { return keysManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
}
