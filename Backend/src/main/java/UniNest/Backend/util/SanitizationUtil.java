package UniNest.Backend.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class SanitizationUtil {

    public static String sanitize(String input) {
        if (input == null) return null;
        return Jsoup.clean(input, Safelist.none());
    }

    /**
     * Specifically for Base64 images.
     * Removes HTML tags but preserves Base64 characters (+, /, =).
     */
    public static String sanitizeBase64(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        // 1. Strip HTML tags
        String clean = Jsoup.clean(input, Safelist.none());
        // 2. Unescape entities (prevents '/' becoming '&#47;')
        return org.jsoup.parser.Parser.unescapeEntities(clean, false);
    }
}