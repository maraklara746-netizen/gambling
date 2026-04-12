package com.mines.plugin.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mines.plugin.MinesPlugin;
import com.mines.plugin.data.KeysManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Tiny HTTP API so the Discord bot (on a different host) can create and query keys.
 * The Minecraft plugin is the single source of truth — keys live in its data folder.
 *
 * Endpoints (all require X-Api-Secret header):
 *   POST /keys/create   { "code": "AAAAA-BBBBB-CCCCC", "amount": 100.0, "createdBy": "Admin#1234" }
 *   GET  /keys/check?code=AAAAA-BBBBB-CCCCC
 *   POST /keys/delete   { "code": "AAAAA-BBBBB-CCCCC" }
 *   GET  /keys/list?filter=all|used|unused
 *   GET  /health
 */
public class KeysHttpServer {

    private final MinesPlugin plugin;
    private final Gson gson = new Gson();
    private HttpServer server;

    public KeysHttpServer(MinesPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() throws IOException {
        int port = plugin.getConfig().getInt("http-port", 4567);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));

        server.createContext("/health",      this::handleHealth);
        server.createContext("/keys/create", this::handleCreate);
        server.createContext("/keys/check",  this::handleCheck);
        server.createContext("/keys/delete", this::handleDelete);
        server.createContext("/keys/list",   this::handleList);

        server.start();
        plugin.getLogger().info("[MinesPlugin] HTTP API started on port " + port);
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private boolean isAuthorized(HttpExchange ex) {
        String secret = plugin.getConfig().getString("http-api-secret", "changeme");
        String header = ex.getRequestHeaders().getFirst("X-Api-Secret");
        return secret.equals(header);
    }

    private void handleHealth(HttpExchange ex) throws IOException {
        respond(ex, 200, "{\"status\":\"ok\"}");
    }

    private void handleCreate(HttpExchange ex) throws IOException {
        if (!isAuthorized(ex)) { respond(ex, 401, err("Unauthorized")); return; }
        if (!"POST".equals(ex.getRequestMethod())) { respond(ex, 405, err("POST required")); return; }
        JsonObject body = readBody(ex);
        if (body == null || !body.has("code") || !body.has("amount")) {
            respond(ex, 400, err("Missing code or amount")); return;
        }
        String code = body.get("code").getAsString().toUpperCase().trim();
        double amount = body.get("amount").getAsDouble();
        String createdBy = body.has("createdBy") ? body.get("createdBy").getAsString() : "Discord";
        String result = plugin.getKeysManager().createKey(code, amount, createdBy);
        if (result == null) { respond(ex, 409, err("Code already exists")); return; }
        JsonObject resp = new JsonObject();
        resp.addProperty("code", code);
        resp.addProperty("amount", amount);
        respond(ex, 200, gson.toJson(resp));
    }

    private void handleCheck(HttpExchange ex) throws IOException {
        if (!isAuthorized(ex)) { respond(ex, 401, err("Unauthorized")); return; }
        String query = ex.getRequestURI().getQuery();
        String code = parseQueryParam(query, "code");
        if (code == null) { respond(ex, 400, err("Missing code param")); return; }
        code = code.toUpperCase().trim();
        Map<String, KeysManager.KeyEntry> keys = plugin.getKeysManager().loadKeys();
        KeysManager.KeyEntry entry = keys.get(code);
        if (entry == null) { respond(ex, 404, err("Code not found")); return; }
        JsonObject item = gson.toJsonTree(entry).getAsJsonObject();
        item.addProperty("code", code);
        respond(ex, 200, gson.toJson(item));
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        if (!isAuthorized(ex)) { respond(ex, 401, err("Unauthorized")); return; }
        if (!"POST".equals(ex.getRequestMethod())) { respond(ex, 405, err("POST required")); return; }
        JsonObject body = readBody(ex);
        if (body == null || !body.has("code")) { respond(ex, 400, err("Missing code")); return; }
        String code = body.get("code").getAsString().toUpperCase().trim();
        Map<String, KeysManager.KeyEntry> keys = plugin.getKeysManager().loadKeys();
        if (!keys.containsKey(code)) { respond(ex, 404, err("Code not found")); return; }
        keys.remove(code);
        plugin.getKeysManager().saveKeys(keys);
        respond(ex, 200, "{\"deleted\":true}");
    }

    private void handleList(HttpExchange ex) throws IOException {
        if (!isAuthorized(ex)) { respond(ex, 401, err("Unauthorized")); return; }
        String filter = parseQueryParam(ex.getRequestURI().getQuery(), "filter");
        if (filter == null) filter = "all";
        Map<String, KeysManager.KeyEntry> keys = plugin.getKeysManager().loadKeys();
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (Map.Entry<String, KeysManager.KeyEntry> e : keys.entrySet()) {
            boolean include = filter.equals("all")
                    || (filter.equals("used") && e.getValue().used)
                    || (filter.equals("unused") && !e.getValue().used);
            if (include) {
                JsonObject item = gson.toJsonTree(e.getValue()).getAsJsonObject();
                item.addProperty("code", e.getKey());
                arr.add(item);
            }
        }
        JsonObject result = new JsonObject();
        result.add("keys", arr);
        result.addProperty("total", arr.size());
        respond(ex, 200, gson.toJson(result));
    }

    private JsonObject readBody(HttpExchange ex) {
        try (InputStream is = ex.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) { return null; }
    }

    private void respond(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private String err(String msg) { return "{\"error\":\"" + msg + "\"}"; }

    private String parseQueryParam(String query, String key) {
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }
}
