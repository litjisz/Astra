package lol.jisz.astra.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Text {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Colors a text using Minecraft color codes (with & as a prefix).
     * Also supports hexadecimal colors in the format &#RRGGBB.
     *
     * @param text Text to colorize
     * @return Colorized text
     */
    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (char c : hex.toCharArray()) {
                replacement.append("&").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Colors a list of texts.
     *
     * @param texts List of texts to colorize
     * @return List of colorized texts
     */
    public static List<String> colorize(List<String> texts) {
        return texts.stream()
                .map(Text::colorize)
                .collect(Collectors.toList());
    }

    /**
     * Converts a text with color codes to a Component (for Paper 1.16+).
     *
     * @param text Text with color codes
     * @return Colorized Component
     */
    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        return LEGACY_SERIALIZER.deserialize(colorize(text));
    }

    /**
     * Converts a text with MiniMessage format to a Component.
     * MiniMessage is a more powerful format than traditional color codes.
     *
     * @param text Text with MiniMessage format
     * @return Formatted Component
     */
    public static Component fromMiniMessage(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        return MINI_MESSAGE.deserialize(text);
    }

    /**
     * Creates a Component with plain text without decorations (bold, italic, etc.).
     *
     * @param text Text with color codes
     * @return Component without decorations
     */
    public static Component plainText(String text) {
        return toComponent(text).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Removes all color codes from a text.
     *
     * @param text Text with color codes
     * @return Text without color codes
     */
    public static String stripColor(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return ChatColor.stripColor(colorize(text));
    }

    /**
     * Creates a color gradient between two hexadecimal colors.
     *
     * @param text Text to apply the gradient
     * @param fromHex Initial hexadecimal color (format: "RRGGBB")
     * @param toHex Final hexadecimal color (format: "RRGGBB")
     * @return Text with color gradient
     */
    public static String gradient(String text, String fromHex, String toHex) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Validate hexadecimal format
        if (!fromHex.matches("[0-9A-Fa-f]{6}") || !toHex.matches("[0-9A-Fa-f]{6}")) {
            return colorize(text);
        }

        int r1 = Integer.parseInt(fromHex.substring(0, 2), 16);
        int g1 = Integer.parseInt(fromHex.substring(2, 4), 16);
        int b1 = Integer.parseInt(fromHex.substring(4, 6), 16);

        int r2 = Integer.parseInt(toHex.substring(0, 2), 16);
        int g2 = Integer.parseInt(toHex.substring(2, 4), 16);
        int b2 = Integer.parseInt(toHex.substring(4, 6), 16);

        char[] chars = text.toCharArray();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == ' ') {
                result.append(' ');
                continue;
            }

            float ratio = (float) i / (chars.length - 1);

            int r = (int) (r1 + (r2 - r1) * ratio);
            int g = (int) (g1 + (g2 - g1) * ratio);
            int b = (int) (b1 + (b2 - b1) * ratio);

            String hex = String.format("%02X%02X%02X", r, g, b);
            result.append("&#").append(hex).append(chars[i]);
        }

        return colorize(result.toString());
    }

    /**
     * Creates a rainbow effect with the provided text.
     *
     * @param text Text to apply the rainbow effect
     * @return Text with rainbow effect
     */
    public static String rainbow(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String[] colors = {
                "FF0000", // Red
                "FF7F00", // Orange
                "FFFF00", // Yellow
                "00FF00", // Green
                "0000FF", // Blue
                "4B0082", // Indigo
                "9400D3"  // Violet
        };

        char[] chars = text.toCharArray();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == ' ') {
                result.append(' ');
                continue;
            }

            String color = colors[i % colors.length];
            result.append("&#").append(color).append(chars[i]);
        }

        return colorize(result.toString());
    }

    /**
     * Centers a text in the Minecraft chat.
     * This method adds appropriate spacing before the text to make it appear centered
     * in the Minecraft chat window based on the specified line length.
     *
     * @param text The text to be centered. Color codes are preserved in the centering calculation.
     * @param lineLength The total character width of the chat window to center within.
     *                  This represents the maximum number of characters that can fit on a line.
     * @return A string with added spaces at the beginning to center the text in chat.
     *         If the text is null or empty, returns an empty string.
     *         If the text is longer than the specified line length, returns the original colorized text.
     */
    public static String center(String text, int lineLength) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String colorized = colorize(text);
        String stripped = ChatColor.stripColor(colorized);

        int spaces = (lineLength - stripped.length()) / 2;
        if (spaces <= 0) {
            return colorized;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            result.append(" ");
        }
        result.append(colorized);

        return result.toString();
    }

    /**
     * Centers a text in the Minecraft chat with a default line length (80).
     *
     * @param text Text to center
     * @return Centered text
     */
    public static String center(String text) {
        return center(text, 80);
    }
}