package top.syshub.relayrace.classic;

import java.util.regex.Pattern;

public final class ClassicText {

    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");

    private ClassicText() {
    }

    public static String toLegacy(String miniMessage) {
        if (miniMessage == null) return "";
        String s = miniMessage;
        s = s.replace("<black>", "\u00A70");
        s = s.replace("<dark_blue>", "\u00A71");
        s = s.replace("<dark_green>", "\u00A72");
        s = s.replace("<dark_aqua>", "\u00A73");
        s = s.replace("<dark_red>", "\u00A74");
        s = s.replace("<dark_purple>", "\u00A75");
        s = s.replace("<gold>", "\u00A76");
        s = s.replace("<gray>", "\u00A77");
        s = s.replace("<dark_gray>", "\u00A78");
        s = s.replace("<blue>", "\u00A79");
        s = s.replace("<green>", "\u00A7a");
        s = s.replace("<aqua>", "\u00A7b");
        s = s.replace("<red>", "\u00A7c");
        s = s.replace("<light_purple>", "\u00A7d");
        s = s.replace("<yellow>", "\u00A7e");
        s = s.replace("<white>", "\u00A7f");
        s = s.replace("<bold>", "\u00A7l");
        s = s.replace("<italic>", "\u00A7o");
        s = s.replace("<underlined>", "\u00A7n");
        s = s.replace("<strikethrough>", "\u00A7m");
        s = s.replace("<reset>", "\u00A7r");
        s = TAG_PATTERN.matcher(s).replaceAll("");
        return s;
    }
}