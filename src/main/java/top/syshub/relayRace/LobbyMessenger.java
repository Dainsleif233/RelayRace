package top.syshub.relayRace;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages BungeeCord plugin messaging for external lobby integration.
 * <p>
 * When a waiting player disconnects and joins an external lobby server (behind
 * a Velocity proxy), this class can recall them when their turn arrives via the
 * {@code ConnectOther} BungeeCord subchannel. The current server's proxy name
 * is auto-detected via {@code GetServer} when the first player joins.
 */
public class LobbyMessenger implements PluginMessageListener {

    private static final String CHANNEL = "BungeeCord";
    private static final long BRINGBACK_TIMEOUT_SECONDS = 3;

    private final RelayRace plugin;
    private final GameManager gameManager;

    private String lobbyServerName;
    @Nullable
    private String detectedServerName;

    /** Players whose bring-back we are waiting for: UUID → future. */
    private final Map<UUID, CompletableFuture<Void>> pendingBringbacks = new ConcurrentHashMap<>();

    public LobbyMessenger(RelayRace plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    // --- Lifecycle ---

    /**
     * Apply config. Server name is auto-detected via {@code GetServer} when a
     * player joins (see {@link #tryAutoDetect}).
     *
     * @param lobbyServerName the name of the lobby server in the proxy config
     */
    public void configure(String lobbyServerName) {
        this.lobbyServerName = lobbyServerName;
    }

    /** Register outgoing and incoming plugin channels. */
    public void register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    /** Unregister channels and cancel any pending bring-backs. */
    public void unregister() {
        try {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        } catch (IllegalArgumentException ignored) {
            // already unregistered
        }
        cancelAllPending();
    }

    // --- Queries ---

    /** @return true if the server name has been detected via GetServer. */
    public boolean isServerNameKnown() {
        return detectedServerName != null;
    }

    // --- Auto-detection ---

    /**
     * Attempt to auto-detect this server's name by sending a {@code GetServer}
     * query to the proxy. Should be called when a player joins the server.
     * <p>
     * The proxy's response is handled in {@link #onPluginMessageReceived}.
     */
    public void tryAutoDetect(Player player) {
        if (!plugin.getRelayConfig().isExternalLobby()) return;
        if (detectedServerName != null) return;
        // 玩家刚加入时 BungeeCord 通道可能尚未就绪，延迟 1 秒发送
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin,
            _ -> tryAutoDetectNow(player), 20L);
    }

    private void tryAutoDetectNow(Player player) {
        if (!plugin.getRelayConfig().isExternalLobby()) return;
        if (detectedServerName != null) return;
        // 如果玩家已离线则跳过
        if (!player.isOnline()) return;
        plugin.debug(plugin.getTranslator().translateRaw("logger.debug.getServerQuery", player.getName()));
        sendBungeeCordMessage(player, out -> out.writeUTF("GetServer"));
    }

    // --- Actions ---

    /**
     * Request the proxy to bring a player back from the external lobby server.
     * <p>
     * Uses the {@code ConnectOther} subchannel. Returns a future that completes
     * when the player arrives back on this server (detected via PlayerJoinEvent),
     * or times out after {@link #BRINGBACK_TIMEOUT_SECONDS} seconds.
     *
     * @param uuid       the player's UUID
     * @param playerName the player's name (used for the BungeeCord command)
     * @return a future that completes normally on arrival, or exceptionally on timeout
     */
    public CompletableFuture<Void> bringBack(UUID uuid, String playerName) {
        if (!plugin.getRelayConfig().isExternalLobby()) {
            return CompletableFuture.failedFuture(new IllegalStateException("外部大厅未启用"));
        }
        if (detectedServerName == null) {
            plugin.getLogger().warning(plugin.getTranslator().translateRaw("logger.bringBack.serverNameUnknown", playerName));
            plugin.debug(plugin.getTranslator().translateRaw("logger.debug.bringBackDebug",
                lobbyServerName, String.valueOf(Bukkit.getOnlinePlayers().size())));
            return CompletableFuture.failedFuture(new IllegalStateException("服务器名称尚未检测"));
        }

        CompletableFuture<Void> future = new CompletableFuture<Void>()
            .orTimeout(BRINGBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        pendingBringbacks.put(uuid, future);

        Player sender = findAnyOnlinePlayer();
        if (sender == null) {
            plugin.getLogger().warning(plugin.getTranslator().translateRaw("logger.bringBack.noOnlinePlayer", playerName));
            pendingBringbacks.remove(uuid);
            return CompletableFuture.failedFuture(new IllegalStateException("没有在线玩家"));
        }

        sendBungeeCordMessage(sender, out -> {
            out.writeUTF("ConnectOther");
            out.writeUTF(playerName);
            out.writeUTF(detectedServerName);
        });

        plugin.debug(plugin.getTranslator().translateRaw("logger.debug.requestSent", playerName, detectedServerName));

        future.whenComplete((v, ex) -> {
            if (ex instanceof CancellationException) {
                // 已取消（如游戏结束），无需处理
            } else if (ex != null) {
                plugin.getLogger().warning(plugin.getTranslator().translateRaw("logger.bringBack.timeout", playerName, String.valueOf(BRINGBACK_TIMEOUT_SECONDS)));
                pendingBringbacks.remove(uuid);
            }
        });

        return future;
    }

    /**
     * Send a chat message to a player who may be on the external lobby server.
     * <p>
     * If the player is online on this server, sends the message directly.
     * Otherwise, uses the BungeeCord {@code Message} subchannel with a
     * {@code [RelayRace]} prefix since formatting is stripped in transit.
     */
    public void sendMessage(Player player, Component message) {
        if (player.isOnline()) {
            player.sendMessage(message);
            return;
        }
        if (!plugin.getRelayConfig().isExternalLobby()) return;

        String plain = PlainTextComponentSerializer.plainText().serialize(message);
        Player sender = findAnyOnlinePlayer();
        if (sender == null) return;

        sendBungeeCordMessage(sender, out -> {
            out.writeUTF("Message");
            out.writeUTF(player.getName());
            out.writeUTF("[RelayRace] " + plain);
        });
    }

    /**
     * Called when a player joins this server — completes any pending bring-back future.
     */
    public void notifyArrived(UUID uuid) {
        CompletableFuture<Void> future = pendingBringbacks.remove(uuid);
        if (future != null) {
            future.complete(null);
        }
    }

    /** Cancel any pending bring-back for a specific player. */
    public void cancelBringBack(UUID uuid) {
        CompletableFuture<Void> future = pendingBringbacks.remove(uuid);
        if (future != null) {
            future.cancel(false);
        }
    }

    /** Cancel all pending bring-backs (e.g. on game end). */
    public void cancelAllPending() {
        for (UUID uuid : Set.copyOf(pendingBringbacks.keySet())) {
            cancelBringBack(uuid);
        }
    }

    // --- PluginMessageListener ---

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String subchannel = in.readUTF();

            if ("GetServer".equals(subchannel)) {
                String name = in.readUTF();
                detectedServerName = name;
                plugin.debug(plugin.getTranslator().translateRaw("logger.debug.getServerResponse", name));
            } else {
                plugin.debug(plugin.getTranslator().translateRaw("logger.debug.unhandledSubchannel", subchannel));
            }
        } catch (IOException e) {
            plugin.getLogger().warning(plugin.getTranslator().translateRaw("logger.bringBack.readError", e.getMessage()));
        }
    }

    // --- Internal helpers ---

    @FunctionalInterface
    private interface MessageWriter {
        void write(DataOutputStream out) throws IOException;
    }

    private void sendBungeeCordMessage(Player player, MessageWriter writer) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             DataOutputStream dataOut = new DataOutputStream(byteOut)) {
            writer.write(dataOut);
            dataOut.flush();
            player.sendPluginMessage(plugin, CHANNEL, byteOut.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning(plugin.getTranslator().translateRaw("logger.bringBack.sendError", e.getMessage()));
        }
    }

    private @Nullable Player findAnyOnlinePlayer() {
        if (gameManager.isRunning() && gameManager.getActivePlayer() != null) {
            return gameManager.getActivePlayer();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            return p;
        }
        return null;
    }
}
