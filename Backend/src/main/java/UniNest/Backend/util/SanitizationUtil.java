package UniNest.Backend.util;
import org.apache.commons.text.StringEscapeUtils;

public class SanitizationUtil {
    // Sanitize input to prevent XSS
    public static String sanitize(String input) {
        if (input == null) return null;
        // Escape HTML
        return StringEscapeUtils.escapeHtml4(input.trim());
    }
}
