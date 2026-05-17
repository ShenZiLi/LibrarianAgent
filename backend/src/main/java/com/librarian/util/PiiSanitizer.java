package com.librarian.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PiiSanitizer {

    private static final Pattern EMAIL_PATTERN = 
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = 
            Pattern.compile("\\b1[3-9]\\d{9}\\b");
    private static final Pattern ID_CARD_PATTERN = 
            Pattern.compile("\\b\\d{17}[0-9Xx]\\b");

    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        result = EMAIL_PATTERN.matcher(result).replaceAll("[邮箱]");
        result = PHONE_PATTERN.matcher(result).replaceAll("[电话]");
        result = ID_CARD_PATTERN.matcher(result).replaceAll("[身份证号]");

        return result;
    }

    public static String sanitizeForLog(String input) {
        String sanitized = sanitize(input);
        if (sanitized != null && sanitized.length() > 100) {
            return sanitized.substring(0, 100) + "...";
        }
        return sanitized;
    }
}
