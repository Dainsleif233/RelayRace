package top.syshub.relayrace.latest;

import top.syshub.relayrace.common.RelayRacePlugin;
import top.syshub.relayrace.common.api.Platform;
import top.syshub.relayrace.common.api.PlatformProvider;

@SuppressWarnings("unused")
public final class LatestPlatformProvider implements PlatformProvider {

    @Override
    public String id() {
        return "latest";
    }

    @Override
    public boolean isCompatible(String bukkitVersion) {
        return bukkitVersion != null && bukkitVersion.startsWith("26.");
    }

    @Override
    public Platform create(RelayRacePlugin plugin) {
        return new LatestPlatform();
    }
}
