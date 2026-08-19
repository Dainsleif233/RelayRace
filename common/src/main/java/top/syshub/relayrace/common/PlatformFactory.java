package top.syshub.relayrace.common;

import top.syshub.relayrace.common.api.Platform;
import top.syshub.relayrace.common.api.PlatformProvider;

public final class PlatformFactory {

    private static final String[] PROVIDERS = {
        "top.syshub.relayrace.latest.LatestPlatformProvider",
        "top.syshub.relayrace.classic.ClassicPlatformProvider"
    };

    private PlatformFactory() {
    }

    public static Platform load(RelayRacePlugin plugin) {
        String version = plugin.getServer().getBukkitVersion();

        for (String providerClass : PROVIDERS) {
            try {
                Class<?> clazz = Class.forName(providerClass);
                PlatformProvider provider =
                    (PlatformProvider) clazz.getConstructor().newInstance();

                if (provider.isCompatible(version)) {
                    plugin.getLogger().info("RelayRace platform: " + provider.id());
                    return provider.create(plugin);
                }
            } catch (ReflectiveOperationException e) {
                plugin.getLogger().warning(
                    "Platform provider unavailable: " + providerClass + ": " + e.getMessage());
            } catch (LinkageError e) {
                plugin.getLogger().warning(
                    "Platform provider cannot be linked: " + providerClass + ": " + e.getMessage());
            }
        }
        return null;
    }
}