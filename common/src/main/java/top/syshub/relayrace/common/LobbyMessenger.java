package top.syshub.relayrace.common;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import top.syshub.relayrace.common.api.Platform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Manages BungeeCord plugin messaging for external lobby integration.
 */
public class LobbyMessenger implements PluginMessageListener {

    private static final String CHANNEL = "BungeeCord";
    private static final long BRINGBACK_TIMEOUT_SECONDS = 3;

    private final RelayRacePlugin plugin;
    private final GameManager gameManager;
    private final Platform platform;

    private String lobbyServerName;
    private String detectedServerName;

    private final Map<UUID, CompletableFuture<Void>> pendingBringbacks =
        new ConcurrentHashMap<UUID, CompletableFuture<Void>>();

    public LobbyMessenger(RelayRacePlugin plugin, GameManager gameManager, Platform platform) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.platform = platform;
    }

    public void configure(String lobbyServerName) {
        this.lobbyServerName = lobbyServerName;
    }

    public void register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public void unregister() {
        try {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        } catch (IllegalArgumentException ignored) {
        }
        cancelAllPending();
    }

    public boolean isServerNameKnown() {
        return detectedServerName != null;
    }

    public void tryAutoDetect(Player player) {
        if (!plugin.getRelayConfig().isExternalLobby()) return;
        if (detectedServerName != null) return;
        platform.scheduler().runDelayed(plugin, new Runnable() {
            @Override
            public void run() {
                tryAutoDetectNow(player);
            }
        }, 20L);
    }

    private void tryAutoDetectNow(Player player) {
        if (!plugin.getRelayConfig().isExternalLobby()) return;
        if (detectedServerName != null) return;
        if (!player.isOnline()) return;
        plugin.debug(plugin.getTranslator().plain("logger.debug.getServerQuery", player.getName()));
        sendBungeeCordMessage(player, new MessageWriter() {
            @Override
            public void write(DataOutputStream out) throws IOException {
                out.writeUTF("GetServer");
            }
        });
    }

    public CompletableFuture<Void> bringBack(UUID uuid, String playerName) {
        if (!plugin.getRelayConfig().isExternalLobby()) {
            return failedFuture(new IllegalStateException("外部大厅未启用"));
        }
        if (detectedServerName == null) {
            plugin.getLogger().warning(plugin.getTranslator().plain("logger.bringBack.serverNameUnknown", playerName));
            plugin.debug(plugin.getTranslator().plain("logger.debug.bringBackDebug",
                lobbyServerName, String.valueOf(Bukkit.getOnlinePlayers().size())));
            return failedFuture(new IllegalStateException("服务器名称尚未检测"));
        }

        final CompletableFuture<Void> future = new CompletableFuture<Void>();
        pendingBringbacks.put(uuid, future);

        Player sender = findAnyOnlinePlayer();
        if (sender == null) {
            plugin.getLogger().warning(plugin.getTranslator().plain("logger.bringBack.noOnlinePlayer", playerName));
            pendingBringbacks.remove(uuid);
            return failedFuture(new IllegalStateException("没有在线玩家"));
        }

        sendBungeeCordMessage(sender, new MessageWriter() {
            @Override
            public void write(DataOutputStream out) throws IOException {
                out.writeUTF("ConnectOther");
                out.writeUTF(playerName);
                out.writeUTF(detectedServerName);
            }
        });

        plugin.debug(plugin.getTranslator().plain("logger.debug.requestSent", playerName, detectedServerName));

        platform.scheduler().runDelayed(plugin, new Runnable() {
            @Override
            public void run() {
                CompletableFuture<Void> f = pendingBringbacks.remove(uuid);
                if (f != null) {
                    f.completeExceptionally(new TimeoutException("bring-back timeout"));
                }
            }
        }, BRINGBACK_TIMEOUT_SECONDS * 20L);

        future.whenComplete(new java.util.function.BiConsumer<Void, Throwable>() {
            @Override
            public void accept(Void v, Throwable ex) {
                if (ex instanceof CancellationException) {
                    // cancelled, nothing to do
                } else if (ex != null) {
                    plugin.getLogger().warning(plugin.getTranslator().plain("logger.bringBack.timeout",
                        playerName, String.valueOf(BRINGBACK_TIMEOUT_SECONDS)));
                    pendingBringbacks.remove(uuid);
                }
            }
        });

        return future;
    }

    public void sendMessage(Player player, String message) {
        if (player.isOnline()) {
            platform.ui().sendMessage(player, message);
            return;
        }
        if (!plugin.getRelayConfig().isExternalLobby()) return;

        String plain = Translator.stripTags(message);
        Player sender = findAnyOnlinePlayer();
        if (sender == null) return;

        sendBungeeCordMessage(sender, new MessageWriter() {
            @Override
            public void write(DataOutputStream out) throws IOException {
                out.writeUTF("Message");
                out.writeUTF(player.getName());
                out.writeUTF("[RelayRace] " + plain);
            }
        });
    }

    public void notifyArrived(UUID uuid) {
        CompletableFuture<Void> future = pendingBringbacks.remove(uuid);
        if (future != null) {
            future.complete(null);
        }
    }

    public void cancelBringBack(UUID uuid) {
        CompletableFuture<Void> future = pendingBringbacks.remove(uuid);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void cancelAllPending() {
        for (UUID uuid : new HashSet<UUID>(pendingBringbacks.keySet())) {
            cancelBringBack(uuid);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String subchannel = in.readUTF();

            if ("GetServer".equals(subchannel)) {
                String name = in.readUTF();
                detectedServerName = name;
                plugin.debug(plugin.getTranslator().plain("logger.debug.getServerResponse", name));
            } else {
                plugin.debug(plugin.getTranslator().plain("logger.debug.unhandledSubchannel", subchannel));
            }
        } catch (IOException e) {
            plugin.getLogger().warning(plugin.getTranslator().plain("logger.bringBack.readError", e.getMessage()));
        }
    }

    private static <T> CompletableFuture<T> failedFuture(Throwable ex) {
        CompletableFuture<T> future = new CompletableFuture<T>();
        future.completeExceptionally(ex);
        return future;
    }

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
            plugin.getLogger().warning(plugin.getTranslator().plain("logger.bringBack.sendError", e.getMessage()));
        }
    }

    private Player findAnyOnlinePlayer() {
        if (gameManager.isRunning() && gameManager.getActivePlayer() != null) {
            return gameManager.getActivePlayer();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            return p;
        }
        return null;
    }
}