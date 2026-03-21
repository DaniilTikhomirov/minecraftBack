package com.back.minecraftback.dto;

/**
 * detailedDescription заполняется только для главных новостей; для мини-новостей при сохранении не используется.
 */
public record NewsDTO(
       Long id,
       String title,
       String description,
       String detailedDescription,
       String date,
       String imageBase64,
       String imageUrl
) {
}
