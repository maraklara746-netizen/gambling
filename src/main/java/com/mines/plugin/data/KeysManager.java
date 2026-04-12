package com.mines.plugin.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mines.plugin.MinesPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class KeysManager {

    private final MinesPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File keysFile;

    public KeysManager(MinesPlugin plugin) {
        this.plugin = plugin;
        // Always resolve the keys file relative to the server root (plugin.getDataFolder().getParentFile())
        // to ensure both the plugin and Discord bot can share the same file reliably.
        String configPath = plugin.getConfig().getString("keys-file", "plugins/MinesPlugin/keys.json");
        File f = new File(configPath);
        if (!f.isAbsolute()) {
            // Resolve relative to server root (parent of plugins folder)
            f = new File(plugin.getDataFolder().getParentFile().getParentFile(), configPath);
        }
        this.keysFile = f;
        plugin.getDataFolder().mkdirs();
        plugin.getLogger().info("Keys file path: " + this.keysFile.getAbsolutePath());
    }

    private Map<String, KeyEntry> loadKeys() {
        if (!keysFile.exists()) return new HashMap<>();
        try (Reader reader = new FileReader(keysFile)) {
            Type type = new TypeToken<Map<String, KeyEntry>>(){}.getType();
            Map<String, KeyEntry> keys = gson.fromJson(reader, type);
            return keys != null ? keys : new HashMap<>();
        } catch (Exception e) {
            plugin.getLogger().warning("Could not read keys file: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private void saveKeys(Map<String, KeyEntry> keys) {
        try {
            keysFile.getParentFile().mkdirs();
            try (Writer writer = new FileWriter(keysFile)) {
                gson.toJson(keys, writer);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save keys file: " + e.getMessage());
        }
    }

    /**
     * Attempts to redeem a code. Returns the amount if successful, -1 if invalid/used.
     */
    public double redeem(String code) {
        code = code.toUpperCase().trim();
        Map<String, KeyEntry> keys = loadKeys();
        KeyEntry entry = keys.get(code);
        if (entry == null) return -1;
        if (entry.used) return -2;

        entry.used = true;
        entry.redeemedAt = System.currentTimeMillis();
        keys.put(code, entry);
        saveKeys(keys);
        return entry.amount;
    }

    public static class KeyEntry {
        public double amount;
        public boolean used;
        public long createdAt;
        public long redeemedAt;
        public String createdBy;

        public KeyEntry() {}
        public KeyEntry(double amount, String createdBy) {
            this.amount = amount;
            this.used = false;
            this.createdAt = System.currentTimeMillis();
            this.createdBy = createdBy;
        }
    }
}
