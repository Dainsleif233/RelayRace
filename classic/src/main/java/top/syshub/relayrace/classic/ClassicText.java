package top.syshub.relayrace.classic;

import java.util.regex.Pattern;

public final class ClassicText {

    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");

    private ClassicText() {
    }

    public static String toLegacy(String miniMessage) {
        if (miniMessage == null) return "";
        String s = miniMessage;
        s = s.replace("<black>", "§0");
        s = s.replace("<dark_blue>", "§1");
        s = s.replace("<dark_green>", "§2");
        s = s.replace("<dark_aqua>", "§3");
        s = s.replace("<dark_red>", "§4");
        s = s.replace("<dark_purple>", "§5");
        s = s.replace("<gold>", "§6");
        s = s.replace("<gray>", "§7");
        s = s.replace("<dark_gray>", "§8");
        s = s.replace("<blue>", "§9");
        s = s.replace("<green>", "§a");
        s = s.replace("<aqua>", "§b");
        s = s.replace("<red>", "§c");
        s = s.replace("<light_purple>", "§d");
        s = s.replace("<yellow>", "§e");
        s = s.replace("<white>", "§f");
        s = s.replace("<bold>", "§l");
        s = s.replace("<italic>", "§o");
        s = s.replace("<underlined>", "§n");
        s = s.replace("<strikethrough>", "§m");
        s = s.replace("<reset>", "§r");
        s = TAG_PATTERN.matcher(s).replaceAll("");
        return s;
    }
}