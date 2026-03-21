package com.back.minecraftback.dto;

public record GetNewsDto(
        Long id,
        String title,
        String description,
        String detailedDescription,
        String date,
        String imageUrl,
        Boolean active
) {
}
