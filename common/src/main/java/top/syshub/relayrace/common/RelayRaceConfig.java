package top.syshub.relayrace.common;

import top.syshub.relayrace.common.api.Platform;

/**
 * RelayRace configuration - loads, caches, and persists all config values.
 */
public class RelayRaceConfig {

    private final RelayRacePlugin plugin;
    private final Platform platform;

    private int turnDuration; // in ticks
    private boolean debug;
    private boolean loop;
    private boolean freeze = true;

    public RelayRaceConfig(RelayRacePlugin plugin, Platform platform) {
        this.plugin = plugin;
        this.platform = platform;
    }

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
        if (externalLobby && (externalLobbyServer == null || externalLobbyServer.isEmpty())) {
            plugin.getLogger().warning(plugin.getTranslator().plain("logger.config.externalLobbyServerMissing"));
            plugin.getLogger().warning(plugin.getTranslator().plain("logger.config.externalLobbyServerHint"));
        }
    }

    public void saveAsync() {
        platform.scheduler().runAsync(plugin, plugin::saveConfig);
    }

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

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        plugin.getConfig().set("debug", debug);
        saveAsync();
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
        plugin.getConfig().set("loop", loop);
        saveAsync();
    }

    public boolean isFreeze() {
        return freeze;
    }

    public void setFreeze(boolean freeze) {
        this.freeze = freeze;
        plugin.getConfig().set("freeze", freeze);
        saveAsync();
    }

    public boolean isExternalLobby() {
        return plugin.getConfig().getBoolean("external-lobby");
    }

    public String getExternalLobbyServer() {
        return plugin.getConfig().getString("external-lobby-server", "");
    }

    public void setExternalLobby(boolean value) {
        plugin.getConfig().set("external-lobby", value);
        saveAsync();
        String server = getExternalLobbyServer();
        if (value && (server == null || server.isEmpty())) {
            plugin.getLogger().warning(plugin.getTranslator().plain("logger.config.externalLobbyServerMissing"));
            plugin.getLogger().warning(plugin.getTranslator().plain("logger.config.externalLobbyServerHint"));
        }
    }

    public void setExternalLobbyServer(String server) {
        plugin.getConfig().set("external-lobby-server", server);
        saveAsync();
        if (isExternalLobby() && (server == null || server.isEmpty())) {
            plugin.getLogger().warning(plugin.getTranslator().plain("logger.config.externalLobbyServerMissing"));
            plugin.getLogger().warning(plugin.getTranslator().plain("logger.config.externalLobbyServerHint"));
        }
    }

    public void setLocale(String locale) {
        plugin.getConfig().set("locale", locale);
        saveAsync();
    }
}
