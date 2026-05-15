package com.librarian.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PiiMaskerTest {

    private final PiiMasker masker = new PiiMasker();

    @Test
    void shouldMaskPhoneNumber() {
        String input = "我的手机号是13812345678";
        String result = masker.mask(input);
        assertEquals("我的手机号是1XX****XXXX", result);
    }

    @Test
    void shouldMaskEmail() {
        String input = "联系邮箱是test@example.com";
        String result = masker.mask(input);
        assertTrue(result.contains("u***@domain"));
    }

    @Test
    void shouldMaskIdCard() {
        String input = "身份证号12345678901234567X";
        String result = masker.mask(input);
        assertTrue(result.contains("******************XX"));
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertNull(masker.mask(null));
    }

    @Test
    void shouldNotModifyTextWithoutPii() {
        String input = "年假有5天";
        assertEquals(input, masker.mask(input));
    }

    @Test
    void shouldDetectPiiInText() {
        assertTrue(masker.containsPii("电话13812345678"));
        assertTrue(masker.containsPii("邮箱test@test.com"));
        assertFalse(masker.containsPii("今天天气很好"));
    }
}
