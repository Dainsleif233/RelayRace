package top.syshub.relayrace.common;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Translator {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z_]+)}");
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final RelayRacePlugin plugin;
    private final Map<String, String> translations = new HashMap<>();
    private String currentLocale;
    private final Set<String> availableLocales =
            new HashSet<>(Arrays.asList("zh", "en"));

    public Translator(RelayRacePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadLocale(String locale) {
        translations.clear();
        if (locale != null && tryLoadLocale(locale)) {
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
        if (translations.containsKey("logger.locale.loaded")) {
            plugin.getLogger().info(plain("logger.locale.loaded", currentLocale));
        } else {
            plugin.getLogger().info("Loaded locale: " + currentLocale);
        }
    }

    private boolean tryLoadLocale(String locale) {
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
     * Return a resolved message string. The string may contain MiniMessage
     * tags (e.g. {@code <green>}); the active platform converts them to its
     * native text format.
     */
    public String format(String key, String... args) {
        String template = translations.get(key);
        if (template == null) {
            return "[missing: " + key + "]";
        }
        return resolvePlaceholders(template, args);
    }

    /**
     * Return a plain-text message with MiniMessage tags removed. Suitable for
     * console logs and BungeeCord plugin-message transport.
     */
    public String plain(String key, String... args) {
        return stripTags(format(key, args));
    }

    public static String stripTags(String input) {
        if (input == null) return "";
        return TAG_PATTERN.matcher(input).replaceAll("");
    }

    private static String resolvePlaceholders(String template, String... args) {
        Matcher m = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        int i = 0;
        while (m.find()) {
            String replacement = i < args.length ? escapeMiniMessage(args[i]) : m.group(0);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            i++;
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String escapeMiniMessage(String input) {
        return input.replace("<", "\\<").replace(">", "\\>");
    }

    public String getCurrentLocale() {
        return currentLocale;
    }

    public Set<String> getAvailableLocales() {
        return availableLocales;
    }
}