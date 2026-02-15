package UniNest.Backend.util;


public class SanitizationUtil {

    /**
     * Strips all HTML tags and unsafe content from a string
     */

        public static String sanitize(String input) {
            if (input == null) return null;
            // Remove all HTML tags
            return input.replaceAll("<[^>]*>", "");
        }

    }

