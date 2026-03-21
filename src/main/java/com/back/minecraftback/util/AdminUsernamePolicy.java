package com.back.minecraftback.util;

import java.util.regex.Pattern;

/**
 * Имя админа в URL/параметрах: только безопасные символы, без path traversal и управляющих символов.
 */
public final class AdminUsernamePolicy {

    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9_.-]{1,64}$");

    private AdminUsernamePolicy() {
    }

    public static String requireValidUsername(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        String trimmed = raw.trim();
        if (!USERNAME.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("username has invalid format (allowed: letters, digits, _ . - , max 64)");
        }
        return trimmed;
    }
}
