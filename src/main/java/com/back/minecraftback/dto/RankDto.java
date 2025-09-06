package com.back.minecraftback.dto;

public record RankDto(
        Long id,
        String title,
        Integer price,
        String description,
        String imageUrl,
        String imageBase64
) {
}
