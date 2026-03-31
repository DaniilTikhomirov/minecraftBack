package com.back.minecraftback.wiki;

import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class WikiImageValidator {

    public static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final Tika tika;

    public void validate(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("empty image data");
        }
        if (data.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("image exceeds max size (" + MAX_IMAGE_BYTES + " bytes)");
        }
        String mime = tika.detect(data);
        if (!ALLOWED_MIME.contains(mime)) {
            throw new IllegalArgumentException("unsupported image type: " + mime);
        }
    }
}
