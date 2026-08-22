package top.syshub.relayrace.common;

import java.io.IOException;
import java.io.InputStream;

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
            if (!isLoadable(providerClass)) {
                continue;
            }
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

    /**
     * Whether the given provider class can be loaded by the current JVM.
     *
     * <p>The jar bundles providers compiled for different Java versions
     * (e.g. the {@code latest} platform targets Java 25 while {@code classic}
     * targets Java 8). On an old server (like MC 1.16 on Java 8), load the
     * class only if its class file version is supported, otherwise even
     * {@link Class#forName(String)} triggers a class-version error.
     */
    private static boolean isLoadable(String className) {
        double maxMajor;
        try {
            maxMajor = Double.parseDouble(System.getProperty("java.class.version", "0"));
        } catch (NumberFormatException ignored) {
            return true;
        }

        String path = '/' + className.replace('.', '/') + ".class";
        try (InputStream in = PlatformFactory.class.getResourceAsStream(path)) {
            if (in == null) {
                // Cannot inspect the class file; let Class.forName decide.
                return true;
            }
            byte[] header = new byte[8];
            int total = 0;
            while (total < header.length) {
                int n = in.read(header, total, header.length - total);
                if (n < 0) {
                    break;
                }
                total += n;
            }
            if (total < header.length) {
                return true;
            }
            // Class file layout: magic(4) minor(2) major(2) — major at [6..8).
            int classFileMajor = ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
            return classFileMajor <= maxMajor;
        } catch (IOException e) {
            return true;
        }
    }
}
