package com.librarian.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class PromptInjectionGuard {

    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(?i)(ignore|forget|override).*(previous|above|all).*(instruction|prompt|rule)"
    );

    private static final Pattern ROLE_PLAY_PATTERN = Pattern.compile(
            "(?i)(act as|pretend to|you are now|role play|扮演|假装)"
    );

    private static final Pattern SYSTEM_ESCAPE_PATTERN = Pattern.compile(
            "(?i)(system prompt|system instruction|\\[SYSTEM\\]|<system>)"
    );

    @Value("${security.prompt-injection.enabled:true}")
    private boolean enabled;

    public boolean isSuspicious(String input) {
        if (!enabled || input == null) {
            return false;
        }
        return INJECTION_PATTERN.matcher(input).find()
                || ROLE_PLAY_PATTERN.matcher(input).find()
                || SYSTEM_ESCAPE_PATTERN.matcher(input).find();
    }

    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        if (isSuspicious(input)) {
            log.warn("Suspicious input detected, sanitized");
        }
        return input.replaceAll("[<>\\[\\]{}|\\\\]", "");
    }
}
