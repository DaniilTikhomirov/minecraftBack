package com.back.minecraftback.wiki.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Тело элемента в POST /wiki (массив из одного объекта). Поле article — объект с version и blocks.
 */
public record WikiCardSaveDto(
        Long id,
        String title,
        String subtitle,
        Integer sortOrder,
        String coverImageUrl,
        String coverImageBase64,
        JsonNode article
) {
}
