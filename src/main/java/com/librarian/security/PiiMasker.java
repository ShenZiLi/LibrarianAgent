package com.librarian.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PiiMasker {

    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\w+@\\w+\\.\\w+");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[\\dXx]");

    public String mask(String text) {
        if (text == null) {
            return null;
        }
        String result = PHONE_PATTERN.matcher(text).replaceAll("1XX****XXXX");
        result = EMAIL_PATTERN.matcher(result).replaceAll("u***@domain");
        result = ID_CARD_PATTERN.matcher(result).replaceAll("******************XX");
        return result;
    }

    public boolean containsPii(String text) {
        if (text == null) {
            return false;
        }
        return PHONE_PATTERN.matcher(text).find()
                || EMAIL_PATTERN.matcher(text).find()
                || ID_CARD_PATTERN.matcher(text).find();
    }
}
