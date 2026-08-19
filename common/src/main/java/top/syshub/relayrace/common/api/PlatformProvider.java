package top.syshub.relayrace.common.api;

import top.syshub.relayrace.common.RelayRacePlugin;

/**
 * Minimal provider loaded reflectively by {@code PlatformFactory}.
 * Implementations must avoid version-specific API types in their own
 * signatures so the class can be loaded on every server.
 */
public interface PlatformProvider {

    String id();

    boolean isCompatible(String bukkitVersion);

    Platform create(RelayRacePlugin plugin);
}