package com.gembud.util;

import org.springframework.stereotype.Component;

/**
 * Utility for sanitizing HTML to prevent XSS attacks.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Component
public class HtmlSanitizer {

    /**
     * Sanitize input string by escaping HTML special characters.
     *
     * @param input raw input string
     * @return sanitized string
     */
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }

        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
    }

    /**
     * Sanitize input string and limit length.
     *
     * @param input raw input string
     * @param maxLength maximum allowed length
     * @return sanitized and truncated string
     */
    public String sanitizeAndLimit(String input, int maxLength) {
        if (input == null) {
            return null;
        }

        String sanitized = sanitize(input);

        if (sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength);
        }

        return sanitized;
    }
}
