package com.mines.plugin.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mines.plugin.MinesPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {

    private final MinesPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File balancesFile;
    private Map<UUID, Double> balances = new HashMap<>();

    public DataManager(MinesPlugin plugin) {
        this.plugin = plugin;
        this.balancesFile = new File(plugin.getDataFolder(), "balances.json");
        plugin.getDataFolder().mkdirs();
        load();
    }

    private void load() {
        if (!balancesFile.exists()) return;
        try (Reader reader = new FileReader(balancesFile)) {
            Type type = new TypeToken<Map<String, Double>>(){}.getType();
            Map<String, Double> raw = gson.fromJson(reader, type);
            if (raw != null) {
                raw.forEach((k, v) -> balances.put(UUID.fromString(k), v));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load balances: " + e.getMessage());
        }
    }

    public void saveAll() {
        try (Writer writer = new FileWriter(balancesFile)) {
            Map<String, Double> raw = new HashMap<>();
            balances.forEach((k, v) -> raw.put(k.toString(), v));
            gson.toJson(raw, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save balances: " + e.getMessage());
        }
    }

    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, plugin.getConfig().getDouble("starting-balance", 100.0));
    }

    public void addBalance(UUID uuid, double amount) {
        double current = getBalance(uuid);
        balances.put(uuid, Math.max(0, current + amount));
        saveAll();
    }

    public void setBalance(UUID uuid, double amount) {
        balances.put(uuid, Math.max(0, amount));
        saveAll();
    }
}
