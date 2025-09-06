package com.back.minecraftback.dto;

public record CasesDto(
        Long id,
        String title,
        String subtitle,
        String description,
        String imageUrl,
        String imageBase64,
        Integer price
        ) {
}
