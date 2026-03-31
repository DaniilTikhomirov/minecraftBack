package com.back.minecraftback.wiki;

import java.util.Base64;

/**
 * Декодирование base64 для вики (без логирования полного содержимого).
 */
public final class WikiBase64Images {

    private WikiBase64Images() {
    }

    public static byte[] decodeImagePayload(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("empty base64");
        }
        String s = raw.trim();
        if (s.startsWith("data:")) {
            int comma = s.indexOf(',');
            if (comma > 0) {
                s = s.substring(comma + 1);
            }
        }
        s = s.replaceAll("\\s+", "");
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid base64 image data", e);
        }
    }
}
