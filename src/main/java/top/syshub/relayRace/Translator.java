package top.syshub.relayRace;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Translator {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z_]+)}");

    private final RelayRace plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> translations = new HashMap<>();
    private String currentLocale;
    private final Set<String> availableLocales = Set.of("zh", "en");

    public Translator(RelayRace plugin) {
        this.plugin = plugin;
    }

    /**
     * Load (or reload) a locale by code. Falls back to zh on failure.
     */
    public void loadLocale(@NotNull String locale) {
        translations.clear();
        if (tryLoadLocale(locale)) {
            currentLocale = locale;
        } else {
            plugin.getLogger().warning(
                "Locale file not found: locales/" + locale + ".yml, falling back to zh");
            if (tryLoadLocale("zh")) {
                currentLocale = "zh";
            } else {
                currentLocale = locale;
                plugin.getLogger().severe("Fallback locale zh not found!");
            }
        }
        // 仅当至少成功加载了一个语言包（primary 或 fallback zh）才能使用翻译器自身
        if (translations.containsKey("logger.locale.loaded")) {
            plugin.getLogger().info(this.translateRaw("logger.locale.loaded", currentLocale));
        } else {
            plugin.getLogger().info("Loaded locale: " + currentLocale);
        }
    }

    /**
     * Attempt to load a locale file from plugin resources.
     *
     * @return true if the file was found and loaded
     */
    private boolean tryLoadLocale(@NotNull String locale) {
        InputStream in = plugin.getResource("locales/" + locale + ".yml");
        if (in == null) return false;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
            new InputStreamReader(in, StandardCharsets.UTF_8));
        for (Map.Entry<String, Object> entry : config.getValues(true).entrySet()) {
            if (entry.getValue() instanceof String) {
                translations.put(entry.getKey(), (String) entry.getValue());
            }
        }
        return true;
    }

    /**
     * Translate a key into a MiniMessage Component.
     *
     * @param key  dotted translation key
     * @param args placeholder values (replaced positionally in order of appearance)
     * @return Adventure Component
     */
    public @NotNull Component translate(@NotNull String key, String @NotNull ... args) {
        String template = translations.get(key);
        if (template == null) {
            return Component.text("[missing: " + key + "]", NamedTextColor.RED);
        }
        String resolved = resolvePlaceholders(template, args);
        return miniMessage.deserialize(resolved);
    }

    /**
     * Translate a key to a plain-text string (MiniMessage tags stripped).
     * Used for APIs that only accept String (e.g. BossBar.setTitle()).
     *
     * @param key  dotted translation key
     * @param args placeholder values (replaced positionally in order of appearance)
     * @return plain text without MiniMessage formatting tags
     */
    public @NotNull String translateRaw(@NotNull String key, String @NotNull ... args) {
        return PlainTextComponentSerializer.plainText().serialize(translate(key, args));
    }

    /**
     * Replace &lt;placeholder&gt; tokens with escaped values, in order of appearance.
     */
    private static String resolvePlaceholders(String template, String... args) {
        Matcher m = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (m.find()) {
            String replacement = i < args.length ? escapeMiniMessage(args[i]) : m.group(0);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            i++;
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Escape MiniMessage special characters so user-provided values are literal text.
     */
    private static String escapeMiniMessage(String input) {
        return input.replace("<", "\\<").replace(">", "\\>");
    }

    public @NotNull String getCurrentLocale() {
        return currentLocale;
    }

    public @NotNull Set<String> getAvailableLocales() {
        return availableLocales;
    }
}
