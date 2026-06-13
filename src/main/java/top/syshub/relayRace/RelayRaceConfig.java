package top.syshub.relayRace;

import org.bukkit.Bukkit;

/**
 * RelayRace configuration — loads, caches, and persists all config values.
 * <p>
 * Owns the config fields that were previously inlined in {@link GameManager}.
 * GameManager accesses config via a reference and adds game-state side effects
 * via its own convenience methods.
 */
public class RelayRaceConfig {

    private final RelayRace plugin;

    private int turnDuration; // in ticks
    private boolean debug;
    private boolean loop;
    private boolean freeze = true;

    public RelayRaceConfig(RelayRace plugin) {
        this.plugin = plugin;
    }

    // --- Load / Save ---

    /** Load values from {@code config.yml} into cached fields. */
    public void load() {
        plugin.getConfig().addDefault("locale", "zh");
        plugin.getConfig().addDefault("time", 300);
        plugin.getConfig().addDefault("debug", false);
        plugin.getConfig().addDefault("loop", true);
        plugin.getConfig().addDefault("freeze", true);
        plugin.getConfig().addDefault("external-lobby", false);
        plugin.getConfig().addDefault("external-lobby-server", "");
        plugin.getConfig().options().copyDefaults(true);
        saveAsync();

        turnDuration = plugin.getConfig().getInt("time") * 20;
        debug = plugin.getConfig().getBoolean("debug");
        loop = plugin.getConfig().getBoolean("loop");
        freeze = plugin.getConfig().getBoolean("freeze");

        boolean externalLobby = plugin.getConfig().getBoolean("external-lobby");
        String externalLobbyServer = plugin.getConfig().getString("external-lobby-server", "");
        if (externalLobby && externalLobbyServer.isEmpty()) {
            plugin.getLogger().warning(plugin.getTranslator().translateRaw("logger.config.externalLobbyServerMissing"));
            plugin.getLogger().warning(plugin.getTranslator().translateRaw("logger.config.externalLobbyServerHint"));
        }
    }

    /** Persist the config file asynchronously. */
    public void saveAsync() {
        Bukkit.getAsyncScheduler().runNow(plugin, _ -> plugin.saveConfig());
    }

    // --- Turn duration ---

    public int getTurnDuration() {
        return turnDuration;
    }

    public int getPlaytimeSeconds() {
        return turnDuration / 20;
    }

    public void setPlaytimeSeconds(int seconds) {
        turnDuration = seconds * 20;
        plugin.getConfig().set("time", seconds);
        saveAsync();
    }

    // --- Debug ---

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        plugin.getConfig().set("debug", debug);
        saveAsync();
    }

    // --- Loop ---

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
        plugin.getConfig().set("loop", loop);
        saveAsync();
    }

    // --- Freeze ---

    public boolean isFreeze() {
        return freeze;
    }

    public void setFreeze(boolean freeze) {
        this.freeze = freeze;
        plugin.getConfig().set("freeze", freeze);
        saveAsync();
    }

    // --- External lobby ---

    public boolean isExternalLobby() {
        return plugin.getConfig().getBoolean("external-lobby");
    }

    public String getExternalLobbyServer() {
        return plugin.getConfig().getString("external-lobby-server", "");
    }

    public void setExternalLobby(boolean value) {
        plugin.getConfig().set("external-lobby", value);
        saveAsync();
        if (value && getExternalLobbyServer().isEmpty()) {
            plugin.getLogger().warning(plugin.getTranslator().translateRaw("logger.config.externalLobbyServerMissing"));
            plugin.getLogger().warning(plugin.getTranslator().translateRaw("logger.config.externalLobbyServerHint"));
        }
    }

    public void setExternalLobbyServer(String server) {
        plugin.getConfig().set("external-lobby-server", server);
        saveAsync();
        if (isExternalLobby() && server.isEmpty()) {
            plugin.getLogger().warning(plugin.getTranslator().translateRaw("logger.config.externalLobbyServerMissing"));
            plugin.getLogger().warning(plugin.getTranslator().translateRaw("logger.config.externalLobbyServerHint"));
        }
    }

    // --- Locale ---

    public void setLocale(String locale) {
        plugin.getConfig().set("locale", locale);
        saveAsync();
    }
}
