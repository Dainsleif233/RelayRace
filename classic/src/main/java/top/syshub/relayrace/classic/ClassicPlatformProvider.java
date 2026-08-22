package top.syshub.relayrace.classic;

import top.syshub.relayrace.common.RelayRacePlugin;
import top.syshub.relayrace.common.api.Platform;
import top.syshub.relayrace.common.api.PlatformProvider;

@SuppressWarnings("unused")
public final class ClassicPlatformProvider implements PlatformProvider {

    @Override
    public String id() {
        return "classic";
    }

    @Override
    public boolean isCompatible(String bukkitVersion) {
        return bukkitVersion != null && bukkitVersion.startsWith("1.16");
    }

    @Override
    public Platform create(RelayRacePlugin plugin) {
        return new ClassicPlatform(plugin);
    }
}
