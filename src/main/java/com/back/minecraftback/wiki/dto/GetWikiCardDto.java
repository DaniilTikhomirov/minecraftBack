package com.back.minecraftback.wiki.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record GetWikiCardDto(
        long id,
        String title,
        String subtitle,
        int sortOrder,
        String coverImageUrl,
        boolean active,
        JsonNode article
) {
}
