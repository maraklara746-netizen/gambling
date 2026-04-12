package com.mines.plugin;

import com.mines.plugin.commands.*;
import com.mines.plugin.game.GameManager;
import com.mines.plugin.data.DataManager;
import com.mines.plugin.data.KeysManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MinesPlugin extends JavaPlugin {

    private static MinesPlugin instance;
    private GameManager gameManager;
    private DataManager dataManager;
    private KeysManager keysManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Init managers
        this.dataManager = new DataManager(this);
        this.keysManager = new KeysManager(this);
        this.gameManager = new GameManager(this);

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

        getLogger().info("MinesPlugin enabled! Good luck and have fun.");
    }

    @Override
    public void onDisable() {
        // Cancel all active games and refund bets
        if (gameManager != null) gameManager.cancelAllGames();
        if (dataManager != null) dataManager.saveAll();
        getLogger().info("MinesPlugin disabled.");
    }

    public static MinesPlugin getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public DataManager getDataManager() { return dataManager; }
    public KeysManager getKeysManager() { return keysManager; }
}
