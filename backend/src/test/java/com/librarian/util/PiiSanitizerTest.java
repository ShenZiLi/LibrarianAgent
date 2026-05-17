package com.librarian.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PiiSanitizerTest {

    @Test
    void shouldSanitizeEmail() {
        String input = "Contact user@example.com for help";
        String result = PiiSanitizer.sanitize(input);
        assertEquals("Contact [邮箱] for help", result);
    }

    @Test
    void shouldSanitizePhone() {
        String input = "Call 13812345678 for support";
        String result = PiiSanitizer.sanitize(input);
        assertEquals("Call [电话] for support", result);
    }

    @Test
    void shouldSanitizeIdCard() {
        String input = "ID: 110101199001011234";
        String result = PiiSanitizer.sanitize(input);
        assertEquals("ID: [身份证号]", result);
    }

    @Test
    void shouldReturnNullForNull() {
        assertNull(PiiSanitizer.sanitize(null));
    }

    @Test
    void shouldReturnEmptyForEmpty() {
        assertEquals("", PiiSanitizer.sanitize(""));
    }

    @Test
    void shouldTruncateForLog() {
        String longText = "A".repeat(150);
        String result = PiiSanitizer.sanitizeForLog(longText);
        assertTrue(result.length() <= 103);
        assertTrue(result.endsWith("..."));
    }
}
